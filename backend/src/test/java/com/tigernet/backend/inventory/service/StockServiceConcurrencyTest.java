package com.tigernet.backend.inventory.service;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.catalog.entity.ProductGroupEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2 phiếu cùng lúc giữ 1 serial: đúng 1 phiếu thành công, phiếu kia bị từ chối 409 INVENTORY_STATUS_INVALID.
 * Phải commit thật để 2 transaction thấy nhau, nên dữ liệu thử được xóa ở finally.
 */
@SpringBootTest
class StockServiceConcurrencyTest {

    @Autowired StockService stockService;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;

    @Test
    void twoConcurrentReservationsOfOneSerialOnlyOneWins() throws Exception {
        String tag = "stk_cc_" + System.nanoTime();
        int[] ids = tx.execute(status -> setUp(tag)); // {warehouseId, groupId, productId, unitId}
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<String> reserve = () -> {
                start.await();
                try {
                    tx.executeWithoutResult(status -> {
                        InventoryEntity unit = em.find(InventoryEntity.class, ids[3]);
                        stockService.moveSerial(unit, InventoryStatus.IN_STOCK, InventoryStatus.RESERVED,
                                StockRef.of(StockRef.EXPORT, 1));
                    });
                    return "OK";
                } catch (AppException e) {
                    return e.getErrorCode().name();
                }
            };
            ExecutorService pool = Executors.newFixedThreadPool(2);
            List<Future<String>> results = new ArrayList<>(List.of(pool.submit(reserve), pool.submit(reserve)));
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> f : results) {
                outcomes.add(f.get());
            }
            pool.shutdown();

            outcomes.sort(null);
            assertEquals(List.of(ErrorCode.INVENTORY_STATUS_INVALID.name(), "OK"), outcomes);
            assertEquals(1, jdbc.queryForObject(
                    "SELECT Qty_Reserved FROM Stock_Summary WHERE Product_ID = ? AND Warehouse_ID = ?",
                    Integer.class, ids[2], ids[0]));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT Qty_On_Hand FROM Stock_Summary WHERE Product_ID = ? AND Warehouse_ID = ?",
                    Integer.class, ids[2], ids[0]));
        } finally {
            jdbc.update("DELETE FROM Stock_Movements WHERE Product_ID = ?", ids[2]);
            jdbc.update("DELETE FROM Inventory WHERE Product_ID = ?", ids[2]);
            jdbc.update("DELETE FROM Stock_Summary WHERE Product_ID = ?", ids[2]);
            jdbc.update("DELETE FROM Products WHERE ID = ?", ids[2]);
            jdbc.update("DELETE FROM Product_Groups WHERE ID = ?", ids[1]);
            jdbc.update("DELETE FROM Warehouses WHERE ID = ?", ids[0]);
        }
    }

    private int[] setUp(String tag) {
        WarehouseEntity w = new WarehouseEntity();
        w.setWarehouseCode(tag.substring(tag.length() - 12));
        w.setWarehouseName(tag);
        w.setAddress("test");
        w.setIsActive(true);
        w.setCreatedAt(LocalDateTime.now());
        em.persist(w);
        ProductGroupEntity g = new ProductGroupEntity();
        g.setGroupName(tag);
        em.persist(g);
        ProductEntity p = new ProductEntity();
        p.setProductName(tag);
        p.setGroup(g);
        p.setUnit("cái");
        p.setDefaultPriceIn(BigDecimal.ONE);
        p.setDefaultPriceOut(BigDecimal.ONE);
        p.setIsSerialized(true);
        p.setMinStockLevel(0);
        em.persist(p);
        em.flush();
        InventoryEntity unit = stockService.receive(p, w, 1, List.of(tag), BigDecimal.ONE, null,
                StockRef.of(StockRef.IMPORT, 1)).get(0);
        return new int[] {w.getId(), g.getId(), p.getId(), unit.getId()};
    }
}
