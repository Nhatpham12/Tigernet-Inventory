package com.tigernet.bff.dashboard;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.common.ApiResponse;
import com.tigernet.bff.common.Json;
import com.tigernet.bff.view.ProductStockService;
import com.tigernet.bff.view.StockStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.tigernet.bff.common.Parallel.async;
import static com.tigernet.bff.common.Parallel.await;

/**
 * Màn "Tổng quan kho" (Figma 965:1641): 4 KPI, mục "Cần xử lý" và "Giao dịch gần nhất".
 * Gộp 5 API backend thành 1 lời gọi cho FE.
 */
@RestController
@RequiredArgsConstructor
public class DashboardController {
    private static final int RECENT_LIMIT = 10;

    private final BackendClient backend;
    private final ProductStockService productStockService;

    public record Kpis(int totalProducts, BigDecimal stockValue, BigDecimal receivables, int lowStockCount) {
    }

    public record Attention(int lowStockCount, int pendingApprovalCount, int overdueRentalCount) {
    }

    public record RecentTransaction(String type, Integer id, String receiptCode, LocalDateTime time,
                                    String partnerName, String warehouseName, int quantity, BigDecimal amount,
                                    String status) {
    }

    public record DashboardView(Kpis kpis, Attention attention, List<RecentTransaction> recentTransactions) {
    }

    @GetMapping("/bff/dashboard")
    ApiResponse<DashboardView> dashboard(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                         @RequestParam(required = false) Integer warehouseId) {
        Map<String, Object> byWarehouse = new HashMap<>();
        if (warehouseId != null) {
            byWarehouse.put("warehouseId", warehouseId);
        }
        CompletableFuture<List<ProductStockService.ProductRow>> products =
                async(() -> productStockService.load(warehouseId, null, auth));
        CompletableFuture<List<JsonNode>> imports = async(() -> backend.getList("/import-receipts", byWarehouse, auth));
        CompletableFuture<List<JsonNode>> exports = async(() -> backend.getList("/export-receipts", byWarehouse, auth));
        CompletableFuture<List<JsonNode>> rentals = async(() -> backend.getList("/rental-receipts", byWarehouse, auth));

        List<ProductStockService.ProductRow> productRows = await(products);
        List<JsonNode> importRows = await(imports);
        List<JsonNode> exportRows = await(exports);
        List<JsonNode> rentalRows = await(rentals);

        int lowStock = (int) productRows.stream()
                .filter(p -> p.stockStatus() == StockStatus.LOW || p.stockStatus() == StockStatus.OUT_OF_STOCK)
                .count();
        BigDecimal stockValue = productRows.stream().map(ProductStockService.ProductRow::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivables = exportRows.stream()
                .filter(r -> !"CANCELLED".equals(Json.text(r, "status")))
                .map(r -> Json.decimal(r, "remain"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pending = (int) Stream.concat(importRows.stream(), exportRows.stream())
                .filter(r -> "PENDING".equals(Json.text(r, "status"))).count()
                + (int) rentalRows.stream().filter(r -> "PENDING".equals(Json.text(r, "status"))).count();
        LocalDateTime now = LocalDateTime.now();
        int overdue = (int) rentalRows.stream()
                .filter(r -> Json.text(r, "returnDate") == null)
                .filter(r -> {
                    LocalDateTime due = Json.dateTime(r, "dueDate");
                    return due != null && due.isBefore(now);
                }).count();

        List<RecentTransaction> recent = new ArrayList<>();
        importRows.forEach(r -> recent.add(new RecentTransaction("IMPORT", Json.integer(r, "id"),
                Json.text(r, "receiptCode"), Json.dateTime(r, "dateIn"), Json.text(r, "sourcePartyName"),
                Json.text(r, "warehouseName"), totalQuantity(r), Json.decimal(r, "totalAmount"), Json.text(r, "status"))));
        exportRows.forEach(r -> recent.add(new RecentTransaction("EXPORT", Json.integer(r, "id"),
                Json.text(r, "receiptCode"), Json.dateTime(r, "dateOut"), Json.text(r, "customerName"),
                Json.text(r, "warehouseName"), totalQuantity(r), Json.decimal(r, "totalAmount"), Json.text(r, "status"))));
        recent.sort(Comparator.comparing(RecentTransaction::time, Comparator.nullsLast(Comparator.reverseOrder())));

        return ApiResponse.success(new DashboardView(
                new Kpis(productRows.size(), stockValue, receivables, lowStock),
                new Attention(lowStock, pending, overdue),
                recent.stream().limit(RECENT_LIMIT).toList()));
    }

    private static int totalQuantity(JsonNode receipt) {
        int total = 0;
        for (JsonNode line : receipt.path("details")) {
            total += Json.intOrZero(line, "quantity");
        }
        return total;
    }
}
