package com.tigernet.backend.inventory.service;

import com.tigernet.backend.alert.service.StockAlertService;
import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.entity.StockMovementEntity;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.enums.MovementType;
import com.tigernet.backend.inventory.repository.InventoryRepository;
import com.tigernet.backend.inventory.repository.StockMovementRepository;
import com.tigernet.backend.inventory.repository.StockSummaryRepository;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock StockSummaryRepository summaryRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock EntityManager entityManager;
    @Mock StockAlertService alertService;
    @InjectMocks StockService stockService;

    final StockRef ref = StockRef.of(StockRef.IMPORT, 7);
    final Map<String, StockSummaryEntity> summaries = new HashMap<>(); // giả lập bảng Stock_Summary theo (SP, kho)
    final List<StockMovementEntity> movements = new ArrayList<>();

    WarehouseEntity main;
    WarehouseEntity branch;
    ProductEntity serialProduct;
    ProductEntity bulkProduct;

    @BeforeEach
    void setUp() {
        main = warehouse(1);
        branch = warehouse(2);
        serialProduct = product(10, true);
        bulkProduct = product(20, false);
        // giả lập: find thường trả dòng đã có (hoặc null), find có khóa tạo dòng 0 khi chưa có; refresh không làm gì
        when(entityManager.find(eq(StockSummaryEntity.class), any())).thenAnswer(inv ->
                summaries.get(key(inv.getArgument(1))));
        when(entityManager.find(eq(StockSummaryEntity.class), any(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenAnswer(inv -> summaryOf(inv.getArgument(1)));
        when(movementRepository.save(any())).thenAnswer(inv -> {
            movements.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    @Test
    void receiveSerialCreatesOneRowPerSerialAndOneInMovement() {
        List<InventoryEntity> rows = stockService.receive(serialProduct, main, 2, List.of("S1", "S2"),
                BigDecimal.TEN, null, ref);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> r.getStatus() == InventoryStatus.IN_STOCK && r.getQuantityOnHand() == 1));
        assertEquals(2, summary(serialProduct, main).getQtyOnHand());
        assertEquals(1, movements.size());
        StockMovementEntity m = movements.get(0);
        assertEquals(MovementType.IN, m.getMovementType());
        assertEquals(2, m.getQuantity());
        assertEquals(2, m.getBalanceAfter());
        assertEquals(StockRef.IMPORT, m.getReferenceType());
        assertEquals(7, m.getReferenceId());
    }

    @Test
    void receiveRejectsWrongSerialCountExistingAndDuplicateSerials() {
        assertCode(ErrorCode.SERIAL_COUNT_MISMATCH,
                () -> stockService.receive(serialProduct, main, 2, List.of("S1"), null, null, ref));
        assertCode(ErrorCode.SERIAL_COUNT_MISMATCH,
                () -> stockService.receive(bulkProduct, main, 1, List.of("S1"), null, null, ref));
        assertCode(ErrorCode.DUPLICATE_SERIAL,
                () -> stockService.receive(serialProduct, main, 2, List.of("S1", "S1"), null, null, ref));
        when(inventoryRepository.existsBySerial("OLD")).thenReturn(true);
        assertCode(ErrorCode.SERIAL_EXISTED,
                () -> stockService.receive(serialProduct, main, 1, List.of("OLD"), null, null, ref));
        verify(inventoryRepository, never()).saveAll(any());
        assertTrue(movements.isEmpty());
    }

    @Test
    void reserveThenSellSerialMovesBetweenColumns() {
        summary(serialProduct, main).setQtyOnHand(3);
        InventoryEntity unit = unit(serialProduct, main, "S1", InventoryStatus.IN_STOCK);
        StockRef export = StockRef.of(StockRef.EXPORT, 5);

        stockService.moveSerial(unit, InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, export);
        assertEquals(2, summary(serialProduct, main).getQtyOnHand());
        assertEquals(1, summary(serialProduct, main).getQtyReserved());
        assertEquals(1, movements.size());
        assertEquals(MovementType.OUT, movements.get(0).getMovementType());
        assertEquals(2, movements.get(0).getBalanceAfter());

        stockService.moveSerial(unit, InventoryStatus.RESERVED, InventoryStatus.SOLD, export);
        assertEquals(0, summary(serialProduct, main).getQtyReserved());
        assertEquals(InventoryStatus.SOLD, unit.getStatus());
        assertTrue(unit.getDateOut() != null);
        assertEquals(1, movements.size()); // hàng đã giữ (không còn khả dụng) bán đi: tồn khả dụng không đổi
    }

    @Test
    void moveSerialRejectsUnitInAnotherStatus() {
        summary(serialProduct, main).setQtyRented(1);
        InventoryEntity unit = unit(serialProduct, main, "S1", InventoryStatus.RENTED);

        assertCode(ErrorCode.INVENTORY_STATUS_INVALID, () -> stockService.moveSerial(unit,
                InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, StockRef.of(StockRef.EXPORT, 5)));
        assertEquals(InventoryStatus.RENTED, unit.getStatus());
    }

    @Test
    void returnRentedUnitToAnotherWarehouse() {
        summary(serialProduct, main).setQtyRented(1);
        InventoryEntity unit = unit(serialProduct, main, "S1", InventoryStatus.RENTED);

        stockService.moveSerial(unit, InventoryStatus.RENTED, InventoryStatus.IN_STOCK, branch,
                StockRef.of(StockRef.RETURN, 3));

        assertEquals(0, summary(serialProduct, main).getQtyRented());
        assertEquals(1, summary(serialProduct, branch).getQtyOnHand());
        assertSame(branch, unit.getWarehouse());
        assertEquals(InventoryStatus.IN_STOCK, unit.getStatus());
        assertEquals(1, movements.size());
        assertSame(branch, movements.get(0).getWarehouse());
    }

    @Test
    void moveQtyTakesOldestLotsFirstAndSplitsTheLast() {
        summary(bulkProduct, main).setQtyOnHand(8);
        InventoryEntity old = lot(bulkProduct, main, 3, LocalDateTime.now().minusDays(2));
        InventoryEntity recent = lot(bulkProduct, main, 5, LocalDateTime.now().minusDays(1));
        when(inventoryRepository.findLotsFifo(20, 1, InventoryStatus.IN_STOCK)).thenReturn(List.of(old, recent));

        List<InventoryEntity> reserved = stockService.moveQty(bulkProduct, main, 4,
                InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, StockRef.of(StockRef.EXPORT, 5));

        assertEquals(InventoryStatus.RESERVED, old.getStatus());
        assertEquals(3, old.getQuantityOnHand());
        assertEquals(InventoryStatus.IN_STOCK, recent.getStatus());
        assertEquals(4, recent.getQuantityOnHand());
        assertEquals(2, reserved.size());
        assertEquals(1, reserved.get(1).getQuantityOnHand());
        assertEquals(recent.getPriceIn(), reserved.get(1).getPriceIn());
        assertEquals(4, summary(bulkProduct, main).getQtyOnHand());
        assertEquals(4, summary(bulkProduct, main).getQtyReserved());
        assertEquals(4, movements.get(0).getQuantity());
    }

    @Test
    void moveQtyFailsWithoutChangesWhenStockIsShort() {
        summary(bulkProduct, main).setQtyOnHand(2);
        InventoryEntity only = lot(bulkProduct, main, 2, LocalDateTime.now());
        when(inventoryRepository.findLotsFifo(20, 1, InventoryStatus.IN_STOCK)).thenReturn(List.of(only));

        assertCode(ErrorCode.INSUFFICIENT_STOCK, () -> stockService.moveQty(bulkProduct, main, 3,
                InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, StockRef.of(StockRef.EXPORT, 5)));
        assertEquals(2, only.getQuantityOnHand());
        assertEquals(2, summary(bulkProduct, main).getQtyOnHand());
        assertTrue(movements.isEmpty());
    }

    @Test
    void adjustAddsLotOrRemovesFromOldest() {
        StockRef adjustment = StockRef.of(StockRef.ADJUSTMENT, 9);
        stockService.adjust(bulkProduct, main, 5, adjustment);
        ArgumentCaptor<List<InventoryEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(inventoryRepository).saveAll(saved.capture());
        assertEquals(5, saved.getValue().get(0).getQuantityOnHand());
        assertEquals(bulkProduct.getDefaultPriceIn(), saved.getValue().get(0).getPriceIn());
        assertEquals(5, summary(bulkProduct, main).getQtyOnHand());

        InventoryEntity lot = saved.getValue().get(0);
        when(inventoryRepository.findLotsFifo(20, 1, InventoryStatus.IN_STOCK)).thenReturn(List.of(lot));
        stockService.adjust(bulkProduct, main, -2, adjustment);
        assertEquals(3, lot.getQuantityOnHand());
        assertEquals(3, summary(bulkProduct, main).getQtyOnHand());
        assertEquals(MovementType.OUT, movements.get(1).getMovementType());
        assertEquals(3, movements.get(1).getBalanceAfter());

        stockService.adjust(bulkProduct, main, 0, adjustment);
        assertEquals(2, movements.size());
        verify(summaryRepository, times(1)).insertIfAbsent(20, 1); // dòng (SP, kho) chỉ tạo lần đầu
    }

    @Test
    void serializedProductCannotUseQuantityMoves() {
        assertCode(ErrorCode.SERIAL_COUNT_MISMATCH, () -> stockService.moveQty(serialProduct, main, 1,
                InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, ref));
        assertCode(ErrorCode.INVALID_REQUEST, () -> stockService.moveQty(bulkProduct, main, 0,
                InventoryStatus.IN_STOCK, InventoryStatus.RESERVED, ref));
    }

    @Test
    void splitLotKeepsAgeAndLocationAndReturnFromSoldClearsDateOut() {
        summary(bulkProduct, main).setQtyOnHand(10);
        LocalDateTime received = LocalDateTime.now().minusDays(30);
        InventoryEntity lot = lot(bulkProduct, main, 10, received);
        lot.setNote("kệ A3");
        when(inventoryRepository.findLotsFifo(20, 1, InventoryStatus.IN_STOCK)).thenReturn(List.of(lot));

        InventoryEntity sold = stockService.moveQty(bulkProduct, main, 4, InventoryStatus.IN_STOCK,
                InventoryStatus.SOLD, StockRef.of(StockRef.EXPORT, 5)).get(0);
        assertEquals(received, sold.getDateIn());
        assertEquals("kệ A3", sold.getNote());
        assertTrue(sold.getDateOut() != null);

        InventoryEntity unit = unit(serialProduct, main, "S1", InventoryStatus.SOLD);
        unit.setDateOut(LocalDateTime.now());
        stockService.moveSerial(unit, InventoryStatus.SOLD, InventoryStatus.IN_STOCK,
                StockRef.of(StockRef.SALES_RETURN, 2));
        assertEquals(null, unit.getDateOut());
    }

    @Test
    void rejectsNoOpMovesAndTrimsSerials() {
        InventoryEntity unit = unit(serialProduct, main, "S1", InventoryStatus.IN_STOCK);
        assertCode(ErrorCode.INVALID_REQUEST, () -> stockService.moveSerial(unit,
                InventoryStatus.IN_STOCK, InventoryStatus.IN_STOCK, ref));
        assertCode(ErrorCode.INVALID_REQUEST, () -> stockService.moveQty(bulkProduct, main, 1,
                InventoryStatus.IN_STOCK, InventoryStatus.IN_STOCK, ref));
        assertTrue(movements.isEmpty());

        assertEquals("SN1", stockService.receive(serialProduct, main, 1, List.of(" SN1 "), null, null, ref)
                .get(0).getSerial());
        assertCode(ErrorCode.DUPLICATE_SERIAL,
                () -> stockService.receive(serialProduct, main, 2, List.of("SN2", "SN2 "), null, null, ref));
    }

    private StockSummaryEntity summary(ProductEntity p, WarehouseEntity w) {
        return summaryOf(new StockSummaryId(p.getId(), w.getId()));
    }

    private StockSummaryEntity summaryOf(StockSummaryId id) {
        return summaries.computeIfAbsent(key(id), k -> {
            StockSummaryEntity s = new StockSummaryEntity();
            s.setId(id);
            return s;
        });
    }

    private static String key(StockSummaryId id) {
        return id.getProductId() + ":" + id.getWarehouseId();
    }

    private static void assertCode(ErrorCode code, Runnable call) {
        assertEquals(code, assertThrows(AppException.class, call::run).getErrorCode());
    }

    private static WarehouseEntity warehouse(int id) {
        WarehouseEntity w = new WarehouseEntity();
        w.setId(id);
        return w;
    }

    private static ProductEntity product(int id, boolean serialized) {
        ProductEntity p = new ProductEntity();
        p.setId(id);
        p.setIsSerialized(serialized);
        p.setDefaultPriceIn(new BigDecimal("1000"));
        return p;
    }

    private static InventoryEntity unit(ProductEntity p, WarehouseEntity w, String serial, InventoryStatus status) {
        InventoryEntity i = new InventoryEntity();
        i.setProduct(p);
        i.setWarehouse(w);
        i.setSerial(serial);
        i.setQuantityOnHand(1);
        i.setStatus(status);
        return i;
    }

    private static InventoryEntity lot(ProductEntity p, WarehouseEntity w, int qty, LocalDateTime dateIn) {
        InventoryEntity i = unit(p, w, null, InventoryStatus.IN_STOCK);
        i.setQuantityOnHand(qty);
        i.setDateIn(dateIn);
        i.setPriceIn(new BigDecimal(qty * 100));
        return i;
    }
}
