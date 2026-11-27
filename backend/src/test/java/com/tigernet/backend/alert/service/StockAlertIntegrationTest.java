package com.tigernet.backend.alert.service;

import com.tigernet.backend.alert.entity.AlertEntity;
import com.tigernet.backend.alert.enums.AlertType;
import com.tigernet.backend.alert.repository.AlertRepository;
import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;
import com.tigernet.backend.rental.dto.request.RentalReceiptCreationRequest;
import com.tigernet.backend.rental.dto.request.RentalReceiptDetailRequest;
import com.tigernet.backend.rental.service.RentalReceiptService;
import com.tigernet.backend.support.TestData;
import com.tigernet.backend.user.service.PageAccessPolicy;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cảnh báo tự động trên MySQL thật: tồn thấp mở/đóng theo biến động kho; nhắc hạn trả hàng thuê. */
@SpringBootTest
@Transactional
class StockAlertIntegrationTest {
    @Autowired StockService stockService;
    @Autowired RentalReceiptService rentalService;
    @Autowired RentDueAlertJob rentDueJob;
    @Autowired AlertRepository alertRepository;
    @Autowired EntityManager em;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lowStockOpensOnceAndClosesWhenRestocked() {
        TestData data = TestData.of(em, "alr_it_");
        WarehouseEntity wh = data.warehouse("W");
        ProductEntity p = data.product("P", false);
        p.setMinStockLevel(3);
        em.flush();
        StockRef ref = StockRef.of(StockRef.IMPORT, 1);

        stockService.receive(p, wh, 5, List.of(), BigDecimal.TEN, null, ref);
        assertEquals(0, openLowStock(p, wh).size());

        stockService.moveQty(p, wh, 2, InventoryStatus.IN_STOCK, InventoryStatus.SOLD, ref); // còn 3 = ngưỡng
        stockService.moveQty(p, wh, 1, InventoryStatus.IN_STOCK, InventoryStatus.SOLD, ref); // còn 2
        assertEquals(1, openLowStock(p, wh).size()); // không mở trùng

        stockService.receive(p, wh, 4, List.of(), BigDecimal.TEN, null, ref); // lên 6
        assertEquals(0, openLowStock(p, wh).size());
    }

    @Test
    void rentDueJobAlertsAndCancelResolves() {
        TestData data = TestData.of(em, "alr_it_");
        data.loginAs(PageAccessPolicy.MANAGER);
        WarehouseEntity wh = data.warehouse("W");
        ProductEntity p = data.product("P", false);
        Integer customerId = data.customer("C").getId();
        em.flush();
        stockService.receive(p, wh, 2, List.of(), BigDecimal.TEN, null, StockRef.of(StockRef.IMPORT, 1));
        Integer rentalId = rentalService.createReceipt(RentalReceiptCreationRequest.builder()
                .customerId(customerId).warehouseId(wh.getId()).dueDate(LocalDateTime.now().plusDays(1))
                .details(List.of(RentalReceiptDetailRequest.builder().productId(p.getId()).quantity(1)
                        .rentalPrice(BigDecimal.ONE).build()))
                .build()).getId();
        em.flush();

        rentDueJob.run();
        rentDueJob.run(); // chạy lại không sinh trùng
        List<AlertEntity> due = openRentDue(rentalId);
        assertEquals(1, due.size());
        assertTrue(due.get(0).getMessage().startsWith("Sắp đến hạn trả"));

        rentalService.cancelReceipt(rentalId);
        em.flush();
        assertEquals(0, openRentDue(rentalId).size());
    }

    private List<AlertEntity> openLowStock(ProductEntity p, WarehouseEntity wh) {
        em.flush();
        return alertRepository.findFirstByAlertTypeAndWarehouseIdAndRefTableAndRefIdAndIsResolvedFalse(
                AlertType.LOW_STOCK, wh.getId(), StockAlertService.REF_PRODUCTS, p.getId()).stream().toList();
    }

    private List<AlertEntity> openRentDue(Integer rentalId) {
        return alertRepository.findByAlertTypeAndRefTableAndRefIdAndIsResolvedFalse(
                AlertType.RENT_DUE, StockAlertService.REF_RENTALS, rentalId);
    }
}
