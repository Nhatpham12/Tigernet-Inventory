package com.tigernet.bff.view;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.common.ApiResponse;
import com.tigernet.bff.common.Json;
import com.tigernet.bff.common.ListQuery;
import com.tigernet.bff.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.tigernet.bff.view.ProductStockService.mapOf;

/**
 * Các màn danh sách trên Figma: tìm kiếm + lọc + sắp xếp + phân trang trả về một lần.
 * Tham số chung: keyword, sort=field,asc|desc, page (từ 1), size.
 */
@RestController
@RequestMapping("/bff")
@RequiredArgsConstructor
public class ViewController {
    private final BackendClient backend;
    private final ProductStockService productStockService;

    // ===== Sản phẩm (968:610) =====
    @GetMapping("/products")
    ApiResponse<PageResponse<ProductStockService.ProductRow>> products(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<ProductStockService.ProductRow> rows = productStockService.load(warehouseId, groupId, auth).stream()
                .filter(r -> stockStatus == null || r.stockStatus() == stockStatus)
                .toList();
        Map<String, Comparator<ProductStockService.ProductRow>> sorts = Map.of(
                "id", Comparator.comparing(ProductStockService.ProductRow::id),
                "productName", Comparator.comparing(ProductStockService.ProductRow::productName, String.CASE_INSENSITIVE_ORDER),
                "priceIn", Comparator.comparing(ProductStockService.ProductRow::priceIn),
                "priceOut", Comparator.comparing(ProductStockService.ProductRow::priceOut),
                "qtyOnHand", Comparator.comparingInt(ProductStockService.ProductRow::qtyOnHand),
                "qtyAvailable", Comparator.comparingInt(ProductStockService.ProductRow::qtyAvailable));
        return ApiResponse.success(ListQuery.of(keyword, sort, page, size).apply(rows,
                r -> r.code() + " " + r.productName() + " " + r.groupName(), sorts::get));
    }

    // ===== Phiếu nhập =====
    @GetMapping("/import-receipts")
    ApiResponse<PageResponse<JsonNode>> importReceipts(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestParam(required = false) Integer warehouseId, @RequestParam(required = false) String status,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = withTotalQuantity(backend.getList("/import-receipts",
                mapOf("warehouseId", warehouseId, "status", status, "from", from, "to", to), auth));
        return page(rows, keyword, sort == null ? "dateIn,desc" : sort, page, size,
                r -> Json.text(r, "receiptCode") + " " + Json.text(r, "sourcePartyName") + " " + Json.text(r, "warehouseName"));
    }

    // ===== Phiếu xuất (978:2335) =====
    @GetMapping("/export-receipts")
    ApiResponse<PageResponse<JsonNode>> exportReceipts(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestParam(required = false) Integer warehouseId, @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = withTotalQuantity(backend.getList("/export-receipts",
                mapOf("warehouseId", warehouseId, "customerId", customerId, "status", status, "from", from, "to", to), auth));
        return page(rows, keyword, sort == null ? "dateOut,desc" : sort, page, size,
                r -> Json.text(r, "receiptCode") + " " + Json.text(r, "customerName") + " " + serials(r));
    }

    // ===== Cho thuê / cho mượn (978:2705, 1112:3135) =====
    @GetMapping("/rental-receipts")
    ApiResponse<PageResponse<JsonNode>> rentalReceipts(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestParam(required = false) Integer warehouseId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        LocalDateTime now = LocalDateTime.now();
        List<JsonNode> rows = withTotalQuantity(backend.getList("/rental-receipts",
                mapOf("warehouseId", warehouseId, "type", type), auth)).stream()
                .map(r -> {
                    // trạng thái thuê để lọc tab Tất cả / Chưa trả / Đã trả / Quá hạn
                    ObjectNode copy = (ObjectNode) r;
                    LocalDateTime due = Json.dateTime(r, "dueDate");
                    String rentalState = Json.text(r, "returnDate") != null ? "RETURNED"
                            : due != null && due.isBefore(now) ? "OVERDUE" : "RENTING";
                    copy.put("rentalState", rentalState);
                    return (JsonNode) copy;
                })
                .filter(r -> state == null || state.isBlank()
                        || state.equals(Json.text(r, "rentalState"))
                        || ("NOT_RETURNED".equals(state) && !"RETURNED".equals(Json.text(r, "rentalState"))))
                .toList();
        return page(rows, keyword, sort == null ? "dateOut,desc" : sort, page, size,
                r -> Json.text(r, "receiptCode") + " " + Json.text(r, "customerName") + " " + serials(r));
    }

    // ===== Báo cáo tồn kho (1005:539) =====
    @GetMapping("/reports/stock")
    ApiResponse<PageResponse<JsonNode>> stockReport(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = backend.getList("/stock-summary", mapOf("warehouseId", warehouseId), auth).stream()
                .map(r -> {
                    ObjectNode copy = (ObjectNode) r;
                    copy.put("productCode", ProductStockService.productCode(Json.integer(r, "productId")));
                    copy.put("qtyAvailable", Math.max(0, Json.intOrZero(r, "qtyOnHand") - Json.intOrZero(r, "qtyReserved")));
                    return (JsonNode) copy;
                }).toList();
        return page(rows, keyword, sort == null ? "productName,asc" : sort, page, size,
                r -> Json.text(r, "productCode") + " " + Json.text(r, "productName") + " " + Json.text(r, "warehouseCode"));
    }

    // ===== Báo cáo nhập xuất kho (1039:480) =====
    @GetMapping("/reports/movements")
    ApiResponse<PageResponse<JsonNode>> movements(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestParam(required = false) Integer warehouseId, @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = backend.getList("/stock-movements",
                mapOf("warehouseId", warehouseId, "productId", productId, "from", from, "to", to), auth);
        return page(rows, keyword, sort == null ? "createdAt,desc" : sort, page, size,
                r -> Json.text(r, "productName") + " " + Json.text(r, "movementType") + " " + Json.text(r, "referenceType"));
    }

    // ===== Công nợ phải thu (1295:2484) =====
    @GetMapping("/reports/receivables")
    ApiResponse<PageResponse<JsonNode>> receivables(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false, defaultValue = "true") boolean onlyOutstanding,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = backend.getList("/export-receipts", mapOf("warehouseId", warehouseId), auth).stream()
                .filter(r -> !"CANCELLED".equals(Json.text(r, "status")))
                .filter(r -> !onlyOutstanding || Json.decimal(r, "remain").compareTo(BigDecimal.ZERO) > 0)
                .toList();
        return page(rows, keyword, sort == null ? "dateOut,desc" : sort, page, size,
                r -> Json.text(r, "receiptCode") + " " + Json.text(r, "customerName"));
    }

    // ===== Danh mục đơn giản: dùng chung tìm kiếm + phân trang =====
    @GetMapping("/suppliers")
    ApiResponse<PageResponse<JsonNode>> suppliers(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                  @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                                  @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return page(backend.getList("/suppliers", Map.of(), auth), keyword, sort, page, size, ViewController::partyText);
    }

    @GetMapping("/customers")
    ApiResponse<PageResponse<JsonNode>> customers(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                  @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                                  @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return page(backend.getList("/customers", Map.of(), auth), keyword, sort, page, size, ViewController::partyText);
    }

    @GetMapping("/alerts")
    ApiResponse<PageResponse<JsonNode>> alerts(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                               @RequestParam(required = false) Integer warehouseId,
                                               @RequestParam(required = false) String alertType,
                                               @RequestParam(required = false) Boolean isResolved,
                                               @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                               @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = backend.getList("/alerts",
                mapOf("warehouseId", warehouseId, "alertType", alertType, "isResolved", isResolved), auth);
        return page(rows, keyword, sort == null ? "createdAt,desc" : sort, page, size,
                r -> Json.text(r, "message") + " " + Json.text(r, "warehouseCode"));
    }

    @GetMapping("/stock-transfers")
    ApiResponse<PageResponse<JsonNode>> transfers(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                                  @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = withTotalQuantity(backend.getList("/stock-transfers", mapOf("status", status), auth));
        return page(rows, keyword, sort == null ? "createdAt,desc" : sort, page, size,
                r -> Json.text(r, "transferCode") + " " + Json.text(r, "fromWarehouseCode") + " " + Json.text(r, "toWarehouseCode"));
    }

    @GetMapping("/stocktakes")
    ApiResponse<PageResponse<JsonNode>> stocktakes(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                   @RequestParam(required = false) Integer warehouseId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                                   @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = backend.getList("/stocktakes", mapOf("warehouseId", warehouseId, "status", status), auth);
        return page(rows, keyword, sort, page, size, JsonNode::toString);
    }

    // Figma 1138:10049 "Đổi trả / Danh sách phiếu"
    @GetMapping("/sales-returns")
    ApiResponse<PageResponse<JsonNode>> salesReturns(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                     @RequestParam(required = false) Integer warehouseId,
                                                     @RequestParam(required = false) String returnType,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                                     @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        List<JsonNode> rows = backend.getList("/sales-returns",
                mapOf("warehouseId", warehouseId, "returnType", returnType, "status", status), auth);
        return page(rows, keyword, sort == null ? "returnDate,desc" : sort, page, size,
                r -> Json.text(r, "receiptCode") + " " + Json.text(r, "exportReceiptCode") + " " + Json.text(r, "partyName"));
    }

    @GetMapping("/warehouses")
    ApiResponse<PageResponse<JsonNode>> warehouses(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                   @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                                   @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return page(backend.getList("/warehouses", Map.of(), auth), keyword, sort, page, size,
                r -> Json.text(r, "warehouseCode") + " " + Json.text(r, "warehouseName") + " " + Json.text(r, "address"));
    }

    @GetMapping("/users")
    ApiResponse<PageResponse<JsonNode>> users(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                              @RequestParam(required = false) String keyword, @RequestParam(required = false) String sort,
                                              @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return page(backend.getList("/users", Map.of(), auth), keyword, sort, page, size,
                r -> Json.text(r, "username") + " " + Json.text(r, "fullName") + " " + Json.text(r, "email"));
    }

    // ===== helpers =====
    private static ApiResponse<PageResponse<JsonNode>> page(List<JsonNode> rows, String keyword, String sort,
                                                            Integer page, Integer size, Function<JsonNode, String> text) {
        return ApiResponse.success(ListQuery.of(keyword, sort, page, size).apply(rows, text, JsonComparators::byField));
    }

    // tổng số lượng các dòng hàng (cột "SL" trên danh sách phiếu)
    private static List<JsonNode> withTotalQuantity(List<JsonNode> receipts) {
        return receipts.stream().map(r -> {
            int total = 0;
            for (JsonNode line : r.path("details")) {
                total += Json.intOrZero(line, "quantity");
            }
            ((ObjectNode) r).put("totalQuantity", total);
            return r;
        }).toList();
    }

    private static String serials(JsonNode receipt) {
        StringBuilder text = new StringBuilder();
        for (JsonNode line : receipt.path("details")) {
            text.append(' ').append(Json.text(line, "productName")).append(' ').append(Json.text(line, "serial"));
        }
        return text.toString();
    }

    private static String partyText(JsonNode r) {
        JsonNode party = r.path("party");
        return Json.text(party, "fullName") + " " + Json.text(party, "taxCode") + " " + Json.text(party, "phone")
                + " " + Json.text(party, "email");
    }
}
