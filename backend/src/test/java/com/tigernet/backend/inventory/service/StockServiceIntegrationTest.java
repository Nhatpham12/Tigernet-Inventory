package com.tigernet.backend.inventory.service;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.catalog.entity.ProductGroupEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.entity.StockMovementEntity;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.enums.MovementType;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Chạy StockService trên MySQL thật (như CI): nhập → giữ → bán → kiểm kê, kiểm tra Stock_Summary,
 * thẻ kho (Balance_After liên tục) và trạng thái serial. Chạy trong transaction và rollback, không để lại dữ liệu.
 */
@SpringBootTest
@Transactional
class StockServiceIntegrationTest {

    @Autowired StockService stockService;
    @Autowired EntityManager em;

    @Test
    void bulkProductFlowKeepsSummaryAndStockCardConsistent() {
        String tag = "stk_it_" + System.nanoTime();
        WarehouseEntity wh = warehouse(tag);
        ProductEntity product = product(tag, false);
        StockRef importRef = StockRef.of(StockRef.IMPORT, 1);
        StockRef exportRef = StockRef.of(StockRef.EXPORT, 1);

        stockService.receive(product, wh, 5, List.of(), new BigDecimal("100"), null, importRef);
        stockService.moveQty(product, wh, 2, InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, exportRef);
        stockService.moveQty(product, wh, 2, InventoryStatus.RESERVED, InventoryStatus.SOLD, exportRef);
        stockService.adjust(product, wh, -1, StockRef.of(StockRef.ADJUSTMENT, 1));
        em.flush();
        em.clear();

        StockSummaryEntity summary = em.find(StockSummaryEntity.class, new StockSummaryId(product.getId(), wh.getId()));
        assertEquals(2, summary.getQtyOnHand());
        assertEquals(0, summary.getQtyReserved());

        List<StockMovementEntity> card = em.createQuery("""
                select m from StockMovementEntity m where m.product.id = :p and m.warehouse.id = :w order by m.id
                """, StockMovementEntity.class)
                .setParameter("p", product.getId()).setParameter("w", wh.getId()).getResultList();
        assertEquals(List.of(MovementType.IN, MovementType.OUT, MovementType.OUT),
                card.stream().map(StockMovementEntity::getMovementType).toList());
        assertEquals(List.of(5, 3, 2), card.stream().map(StockMovementEntity::getBalanceAfter).toList());

        Long inStock = em.createQuery("""
                select coalesce(sum(i.quantityOnHand), 0) from InventoryEntity i
                where i.product.id = :p and i.warehouse.id = :w and i.status = :s
                """, Long.class).setParameter("p", product.getId()).setParameter("w", wh.getId())
                .setParameter("s", InventoryStatus.IN_STOCK).getSingleResult();
        assertEquals(2L, inStock); // nguồn sự thật Inventory khớp bộ đệm Stock_Summary
    }

    @Test
    void serialUnitGoesInStockReservedSoldAndCannotBeSoldTwice() {
        String tag = "stk_it_" + System.nanoTime();
        WarehouseEntity wh = warehouse(tag);
        ProductEntity product = product(tag, true);
        StockRef exportRef = StockRef.of(StockRef.EXPORT, 2);

        InventoryEntity unit = stockService.receive(product, wh, 1, List.of(tag + "-SN"), BigDecimal.TEN, null,
                StockRef.of(StockRef.IMPORT, 2)).get(0);
        stockService.moveSerial(unit, InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, exportRef);
        stockService.moveSerial(unit, InventoryStatus.RESERVED, InventoryStatus.SOLD, exportRef);
        em.flush();
        em.clear();

        InventoryEntity reloaded = em.find(InventoryEntity.class, unit.getId());
        assertEquals(InventoryStatus.SOLD, reloaded.getStatus());
        AppException again = assertThrows(AppException.class, () -> stockService.moveSerial(reloaded,
                InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, StockRef.of(StockRef.EXPORT, 3)));
        assertEquals(ErrorCode.INVENTORY_STATUS_INVALID, again.getErrorCode());

        StockSummaryEntity summary = em.find(StockSummaryEntity.class, new StockSummaryId(product.getId(), wh.getId()));
        assertEquals(0, summary.getQtyOnHand());
        assertEquals(0, summary.getQtyReserved());
    }

    private WarehouseEntity warehouse(String tag) {
        WarehouseEntity w = new WarehouseEntity();
        w.setWarehouseCode(tag.substring(tag.length() - 12));
        w.setWarehouseName(tag);
        w.setAddress("test");
        w.setIsActive(true);
        w.setCreatedAt(LocalDateTime.now());
        em.persist(w);
        return w;
    }

    private ProductEntity product(String tag, boolean serialized) {
        ProductGroupEntity group = new ProductGroupEntity();
        group.setGroupName(tag);
        em.persist(group);
        ProductEntity p = new ProductEntity();
        p.setProductName(tag);
        p.setGroup(group);
        p.setUnit("cái");
        p.setDefaultPriceIn(new BigDecimal("100"));
        p.setDefaultPriceOut(new BigDecimal("150"));
        p.setIsSerialized(serialized);
        p.setMinStockLevel(0);
        em.persist(p);
        em.flush();
        return p;
    }
}
