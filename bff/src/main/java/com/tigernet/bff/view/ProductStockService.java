package com.tigernet.bff.view;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.common.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.tigernet.bff.common.Parallel.async;
import static com.tigernet.bff.common.Parallel.await;

// ghép danh mục sản phẩm với tồn kho (Stock_Summary) theo kho đang chọn
@Service
@RequiredArgsConstructor
public class ProductStockService {
    private final BackendClient backend;

    public record ProductRow(Integer id, String code, String productName, Integer groupId, String groupName,
                             String unit, BigDecimal priceIn, BigDecimal priceOut, Boolean isSerialized,
                             Integer minStockLevel, String imagePath, int qtyOnHand, int qtyAvailable,
                             int qtyRented, int qtyDamaged, BigDecimal stockValue, StockStatus stockStatus) {
    }

    public List<ProductRow> load(Integer warehouseId, Integer groupId, String authorization) {
        CompletableFuture<List<JsonNode>> products = async(() ->
                backend.getList("/products", mapOf("groupId", groupId), authorization));
        CompletableFuture<List<JsonNode>> stock = async(() ->
                backend.getList("/stock-summary", mapOf("warehouseId", warehouseId), authorization));

        // không chọn kho = cộng dồn tồn của mọi kho
        Map<Integer, int[]> qtyByProduct = new HashMap<>();
        for (JsonNode row : await(stock)) {
            int[] qty = qtyByProduct.computeIfAbsent(Json.integer(row, "productId"), k -> new int[4]);
            qty[0] += Json.intOrZero(row, "qtyOnHand");
            qty[1] += Json.intOrZero(row, "qtyReserved");
            qty[2] += Json.intOrZero(row, "qtyRented");
            qty[3] += Json.intOrZero(row, "qtyDamaged");
        }

        return await(products).stream().map(p -> {
            Integer id = Json.integer(p, "id");
            int[] qty = qtyByProduct.getOrDefault(id, new int[4]);
            int available = Math.max(0, qty[0] - qty[1]);
            BigDecimal priceOut = Json.decimal(p, "defaultPriceOut");
            Integer min = Json.integer(p, "minStockLevel");
            return new ProductRow(id, productCode(id), Json.text(p, "productName"), Json.integer(p, "groupId"),
                    Json.text(p, "groupName"), Json.text(p, "unit"), Json.decimal(p, "defaultPriceIn"), priceOut,
                    p.path("isSerialized").asBoolean(false), min, Json.text(p, "imagePath"), qty[0], available,
                    qty[2], qty[3], priceOut.multiply(BigDecimal.valueOf(qty[0])), StockStatus.of(available, min));
        }).toList();
    }

    // backend chưa có cột SKU; hiển thị mã theo ID như mẫu Figma (SP-00124)
    public static String productCode(Integer id) {
        return id == null ? "" : "SP-%05d".formatted(id);
    }

    static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                map.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        return map;
    }
}
