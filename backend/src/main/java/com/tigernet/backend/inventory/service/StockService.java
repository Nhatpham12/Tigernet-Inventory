package com.tigernet.backend.inventory.service;

import com.tigernet.backend.alert.service.StockAlertService;
import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inbound.entity.ImportReceiptDetailEntity;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Nơi DUY NHẤT ghi Inventory / Stock_Summary / Stock_Movements (Database_Design_v7, mục Stock_Movements).
 * Luôn chạy trong transaction của phiếu gọi tới (MANDATORY): phiếu lỗi thì tồn kho rollback theo.
 *
 * - Inventory là nguồn sự thật: hàng serial 1 dòng = 1 đơn vị; hàng không serial 1 dòng = 1 lô (Quantity_On_Hand).
 * - Stock_Summary là bộ đệm số lượng theo trạng thái của (SP, kho). Mỗi biến động khóa dòng này trước
 *   (đọc có khóa FOR UPDATE, luôn lấy số mới nhất) nên 2 phiếu cùng SP/kho chạy lần lượt, không bán trùng.
 * - Qty_On_Hand = hàng IN_STOCK = tồn khả dụng. Stock_Movements chỉ ghi khi số này đổi,
 *   Balance_After = Qty_On_Hand ngay sau biến động (thẻ kho).
 *
 * Quy tắc cho nơi gọi: khóa giữ tới hết transaction, nên phiếu nhiều dòng phải gọi theo thứ tự
 * (productId, warehouseId) tăng dần — 2 phiếu cùng thứ tự thì không khóa chéo nhau (deadlock).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(propagation = Propagation.MANDATORY)
public class StockService {
    InventoryRepository inventoryRepository;
    StockSummaryRepository summaryRepository;
    StockMovementRepository movementRepository;
    EntityManager entityManager;
    StockAlertService alertService;

    /**
     * Đưa hàng mới vào kho ở trạng thái IN_STOCK (phiếu nhập xác nhận, kiểm kê thừa).
     * Hàng serial: đúng qty serial (bỏ khoảng trắng đầu/cuối), mỗi serial 1 dòng. Hàng không serial: 1 lô.
     */
    public List<InventoryEntity> receive(ProductEntity product, WarehouseEntity warehouse, int qty, List<String> serials,
                                         BigDecimal priceIn, ImportReceiptDetailEntity importDetail, StockRef ref) {
        requirePositive(qty);
        List<String> serialList = normalizeSerials(product, qty, serials);

        StockSummaryEntity summary = lockSummary(product, warehouse);
        List<InventoryEntity> created = new ArrayList<>();
        if (serialList.isEmpty()) {
            created.add(newRow(product, warehouse, null, qty, InventoryStatus.IN_STOCK, priceIn, importDetail));
        } else {
            for (String serial : serialList) {
                created.add(newRow(product, warehouse, serial, 1, InventoryStatus.IN_STOCK, priceIn, importDetail));
            }
        }
        inventoryRepository.saveAll(created);
        shift(summary, InventoryStatus.IN_STOCK, qty, ref);
        return created;
    }

    /** Đổi trạng thái 1 đơn vị serial trong cùng kho, VD IN_STOCK → RESERVED (duyệt xuất), RESERVED → SOLD. */
    public void moveSerial(InventoryEntity unit, InventoryStatus from, InventoryStatus to, StockRef ref) {
        moveSerial(unit, from, to, unit.getWarehouse(), ref);
    }

    /**
     * Đổi trạng thái 1 đơn vị serial, có thể sang kho khác (VD trả hàng thuê về kho khác: RENTED ở kho A → IN_STOCK ở kho B).
     * Khóa Stock_Summary trước rồi mới đọc lại đơn vị từ DB: phiếu khác vừa đổi đơn vị này thì thấy ngay và báo 409,
     * không ghi đè. Thay đổi chưa lưu trên {@code unit} của nơi gọi sẽ bị bỏ khi đọc lại.
     */
    public void moveSerial(InventoryEntity unit, InventoryStatus from, InventoryStatus to,
                           WarehouseEntity toWarehouse, StockRef ref) {
        if (unit.getSerial() == null) {
            throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH);
        }
        WarehouseEntity fromWarehouse = unit.getWarehouse();
        boolean sameWarehouse = sameWarehouse(fromWarehouse, toWarehouse);
        if (from == to && sameWarehouse) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        StockSummaryEntity[] pair = lockPair(unit.getProduct(), fromWarehouse, toWarehouse);
        StockSummaryEntity source = pair[0];
        StockSummaryEntity target = pair[1];

        lockLatest(unit);
        if (unit.getStatus() != from || !sameWarehouse(unit.getWarehouse(), fromWarehouse)) {
            throw new AppException(ErrorCode.INVENTORY_STATUS_INVALID);
        }
        shift(source, from, -1, ref);
        shift(target, to, 1, ref);
        if (!sameWarehouse) {
            unit.setWarehouse(toWarehouse);
            unit.setLocation(null); // vị trí kệ của kho cũ không còn đúng
        }
        unit.setStatus(to);
        unit.setDateOut(to == InventoryStatus.SOLD ? LocalDateTime.now() : null);
        inventoryRepository.save(unit);
    }

    /**
     * Chuyển qty hàng KHÔNG serial giữa 2 trạng thái trong cùng kho, lấy lô cũ trước (FIFO).
     * Lô lấy hết thì đổi trạng thái cả lô; lấy một phần thì tách lô (giữ ngày nhập, giá nhập, vị trí, dòng nhập gốc).
     * {@code to == null}: bỏ hẳn khỏi kho (kiểm kê thiếu). Thiếu hàng thì 409 và không đổi gì.
     * Trả về các dòng ở trạng thái mới (rỗng nếu to == null).
     */
    public List<InventoryEntity> moveQty(ProductEntity product, WarehouseEntity warehouse, int qty,
                                         InventoryStatus from, InventoryStatus to, StockRef ref) {
        return moveQty(product, warehouse, warehouse, qty, from, to, ref);
    }

    /**
     * Như trên nhưng các lô lấy ra có thể chuyển sang kho khác (VD trả hàng thuê không serial về kho khác:
     * RENTED ở kho A → IN_STOCK/DAMAGED ở kho B). Lô sang kho mới bỏ vị trí kệ cũ. {@code to == null} chỉ dùng cùng kho.
     */
    public List<InventoryEntity> moveQty(ProductEntity product, WarehouseEntity fromWarehouse,
                                         WarehouseEntity toWarehouse, int qty,
                                         InventoryStatus from, InventoryStatus to, StockRef ref) {
        requirePositive(qty);
        boolean sameWarehouse = sameWarehouse(fromWarehouse, toWarehouse);
        if ((from == to && sameWarehouse) || (to == null && !sameWarehouse)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (Boolean.TRUE.equals(product.getIsSerialized())) {
            throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH);
        }
        StockSummaryEntity[] pair = lockPair(product, fromWarehouse, toWarehouse);
        List<InventoryEntity> lots = inventoryRepository.findLotsFifo(product.getId(), fromWarehouse.getId(), from);
        int available = lots.stream().mapToInt(InventoryEntity::getQuantityOnHand).sum();
        if (available < qty) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        List<InventoryEntity> moved = new ArrayList<>();
        int remaining = qty;
        for (InventoryEntity lot : lots) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, lot.getQuantityOnHand());
            remaining -= take;
            if (to == null) {
                lot.setQuantityOnHand(lot.getQuantityOnHand() - take);
                continue;
            }
            InventoryEntity target = lot;
            if (take < lot.getQuantityOnHand()) {
                lot.setQuantityOnHand(lot.getQuantityOnHand() - take);
                target = splitLot(lot, take);
            }
            target.setStatus(to);
            target.setDateOut(to == InventoryStatus.SOLD ? LocalDateTime.now() : null);
            if (!sameWarehouse) {
                target.setWarehouse(toWarehouse);
                target.setLocation(null); // vị trí kệ của kho cũ không còn đúng
            }
            moved.add(target);
        }
        inventoryRepository.saveAll(lots);
        inventoryRepository.saveAll(moved);
        shift(pair[0], from, -qty, ref);
        if (to != null) {
            shift(pair[1], to, qty, ref);
        }
        return moved;
    }

    /** khóa Stock_Summary của (SP, kho đi) và (SP, kho đến) theo id kho tăng dần để không khóa chéo; cùng kho thì 1 dòng */
    private StockSummaryEntity[] lockPair(ProductEntity product, WarehouseEntity fromWarehouse,
                                          WarehouseEntity toWarehouse) {
        if (sameWarehouse(fromWarehouse, toWarehouse)) {
            StockSummaryEntity summary = lockSummary(product, fromWarehouse);
            return new StockSummaryEntity[]{summary, summary};
        }
        if (fromWarehouse.getId() < toWarehouse.getId()) {
            StockSummaryEntity source = lockSummary(product, fromWarehouse);
            return new StockSummaryEntity[]{source, lockSummary(product, toWarehouse)};
        }
        StockSummaryEntity target = lockSummary(product, toWarehouse);
        return new StockSummaryEntity[]{lockSummary(product, fromWarehouse), target};
    }

    /** Điều chỉnh tồn khả dụng hàng không serial theo chênh lệch kiểm kê: dương thêm lô mới, âm bớt theo FIFO. */
    public void adjust(ProductEntity product, WarehouseEntity warehouse, int signedQty, StockRef ref) {
        if (signedQty > 0) {
            receive(product, warehouse, signedQty, List.of(), product.getDefaultPriceIn(), null, ref);
        } else if (signedQty < 0) {
            moveQty(product, warehouse, -signedQty, InventoryStatus.IN_STOCK, null, ref);
        }
    }

    /**
     * Khóa dòng Stock_Summary của (SP, kho) tới hết transaction và trả về số mới nhất.
     * Đã có dòng (kể cả đang nằm trong bộ nhớ đệm Hibernate): khóa và nạp số mới nhất ({@link #lockLatest}).
     * Chưa có: tạo dòng 0 rồi đọc có khóa (đọc có khóa thấy cả dòng transaction khác vừa tạo).
     */
    private StockSummaryEntity lockSummary(ProductEntity product, WarehouseEntity warehouse) {
        StockSummaryId id = new StockSummaryId(product.getId(), warehouse.getId());
        StockSummaryEntity summary = entityManager.find(StockSummaryEntity.class, id);
        if (summary != null) {
            lockLatest(summary);
        } else {
            summaryRepository.insertIfAbsent(product.getId(), warehouse.getId());
            summary = entityManager.find(StockSummaryEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        }
        summary.setProduct(product);
        summary.setWarehouse(warehouse);
        return summary;
    }

    /**
     * Khóa dòng của entity đang quản lý và nạp số mới nhất từ DB.
     * Đã khóa trong transaction này thì bản trong bộ nhớ là mới nhất, không đọc lại (đọc lại sẽ mất thay đổi chưa ghi).
     * Chưa khóa: ghi thay đổi đang chờ của transaction xuống trước, rồi đọc lại có khóa.
     */
    private void lockLatest(Object entity) {
        if (entityManager.getLockMode(entity) != LockModeType.PESSIMISTIC_WRITE) {
            entityManager.flush();
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        }
    }

    /**
     * Cộng/trừ delta vào cột ứng với trạng thái; SOLD là hàng đã ra khỏi kho nên không có cột.
     * Đổi Qty_On_Hand (IN_STOCK) thì ghi 1 dòng thẻ kho.
     */
    private void shift(StockSummaryEntity summary, InventoryStatus status, int delta, StockRef ref) {
        switch (status) {
            case IN_STOCK -> summary.setQtyOnHand(checked(summary.getQtyOnHand() + delta));
            case RESERVED -> summary.setQtyReserved(checked(summary.getQtyReserved() + delta));
            case RENTED, LENT -> summary.setQtyRented(checked(summary.getQtyRented() + delta));
            case DAMAGED -> summary.setQtyDamaged(checked(summary.getQtyDamaged() + delta));
            case IN_TRANSIT -> summary.setQtyInTransit(checked(summary.getQtyInTransit() + delta));
            case SOLD -> { }
        }
        summary.setLastUpdated(LocalDateTime.now());
        summaryRepository.save(summary);
        if (status == InventoryStatus.IN_STOCK && delta != 0) {
            StockMovementEntity movement = new StockMovementEntity();
            movement.setProduct(summary.getProduct());
            movement.setWarehouse(summary.getWarehouse());
            movement.setMovementType(delta > 0 ? MovementType.IN : MovementType.OUT);
            movement.setQuantity(Math.abs(delta));
            movement.setBalanceAfter(summary.getQtyOnHand());
            movement.setReferenceType(ref.type());
            movement.setReferenceId(ref.id());
            movement.setCreatedAt(LocalDateTime.now());
            movementRepository.save(movement);
            // tồn khả dụng vừa đổi: mở/đóng cảnh báo tồn thấp của (SP, kho)
            alertService.refreshLowStock(summary.getProduct(), summary.getWarehouse(), summary.getQtyOnHand());
        }
    }

    // bộ đệm lệch xuống âm nghĩa là đang lấy nhiều hơn số có: chặn lại thay vì ghi số sai
    private static int checked(int value) {
        if (value < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        return value;
    }

    // hàng serial cần đúng qty serial khác nhau, chưa có trong kho; hàng không serial không nhận serial
    private List<String> normalizeSerials(ProductEntity product, int qty, List<String> serials) {
        List<String> result = new ArrayList<>();
        for (String serial : serials == null ? List.<String>of() : serials) {
            if (serial == null || serial.isBlank()) {
                throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH);
            }
            result.add(serial.trim());
        }
        boolean serialized = Boolean.TRUE.equals(product.getIsSerialized());
        if (serialized ? result.size() != qty : !result.isEmpty()) {
            throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH);
        }
        Set<String> seen = new HashSet<>();
        for (String serial : result) {
            if (!seen.add(serial)) {
                throw new AppException(ErrorCode.DUPLICATE_SERIAL);
            }
            if (inventoryRepository.existsBySerial(serial)) {
                throw new AppException(ErrorCode.SERIAL_EXISTED);
            }
        }
        return result;
    }

    private static InventoryEntity splitLot(InventoryEntity lot, int qty) {
        InventoryEntity part = newRow(lot.getProduct(), lot.getWarehouse(), null, qty, lot.getStatus(),
                lot.getPriceIn(), lot.getImportDetail());
        part.setDateIn(lot.getDateIn()); // giữ tuổi lô để FIFO và báo cáo tuổi hàng đúng
        part.setLocation(lot.getLocation());
        part.setNote(lot.getNote());
        return part;
    }

    private static InventoryEntity newRow(ProductEntity product, WarehouseEntity warehouse, String serial, int qty,
                                          InventoryStatus status, BigDecimal priceIn,
                                          ImportReceiptDetailEntity importDetail) {
        InventoryEntity row = new InventoryEntity();
        row.setProduct(product);
        row.setWarehouse(warehouse);
        row.setSerial(serial);
        row.setQuantityOnHand(qty);
        row.setStatus(status);
        row.setPriceIn(priceIn);
        row.setImportDetail(importDetail);
        row.setDateIn(LocalDateTime.now());
        return row;
    }

    private static boolean sameWarehouse(WarehouseEntity a, WarehouseEntity b) {
        return Objects.equals(a.getId(), b.getId());
    }

    private static void requirePositive(int qty) {
        if (qty <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}
