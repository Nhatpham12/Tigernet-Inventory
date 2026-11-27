package com.tigernet.backend.stocktake.service;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.entity.StockMovementEntity;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import com.tigernet.backend.inventory.enums.MovementType;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;
import com.tigernet.backend.stocktake.dto.request.StocktakeCompleteRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeCreationRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeDetailRequest;
import com.tigernet.backend.stocktake.enums.StocktakeStatus;
import com.tigernet.backend.support.TestData;
import com.tigernet.backend.user.service.PageAccessPolicy;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Chốt kiểm kê trên MySQL thật: chênh lệch áp vào tồn, có thẻ kho; serial lệch và thiếu lý do bị chặn. */
@SpringBootTest
@Transactional
class StocktakeCompleteIntegrationTest {
    @Autowired StocktakeService service;
    @Autowired StockService stockService;
    @Autowired EntityManager em;

    TestData data;
    WarehouseEntity wh;
    ProductEntity shortage;   // sổ 5, đếm được 3
    ProductEntity opening;    // sổ 0, đếm được 4 (tồn đầu kỳ)

    @BeforeEach
    void setUp() {
        data = TestData.of(em, "stk_it_");
        data.loginAs(PageAccessPolicy.MANAGER);
        wh = data.warehouse("W");
        shortage = data.product("A", false);
        opening = data.product("B", false);
        em.flush();
        stockService.receive(shortage, wh, 5, List.of(), BigDecimal.TEN, null, StockRef.of(StockRef.IMPORT, 1));
        em.flush();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeAppliesDifferencesToStock() {
        Integer id = create(Map.of(shortage, 3, opening, 4));
        StocktakeCompleteRequest request = new StocktakeCompleteRequest();
        request.setReasons(Map.of(shortage.getId(), "hư hỏng", opening.getId(), "tồn đầu kỳ"));

        assertEquals(StocktakeStatus.COMPLETED, service.completeStocktake(id, request).getStatus());
        em.flush();
        em.clear();

        assertEquals(3, summary(shortage).getQtyOnHand());
        assertEquals(4, summary(opening).getQtyOnHand());
        List<StockMovementEntity> moves = em.createQuery(
                "select m from StockMovementEntity m where m.referenceType = :t and m.warehouse.id = :w order by m.id",
                StockMovementEntity.class)
                .setParameter("t", StockRef.ADJUSTMENT).setParameter("w", wh.getId()).getResultList();
        assertEquals(List.of(MovementType.OUT, MovementType.IN),
                moves.stream().map(StockMovementEntity::getMovementType).toList());
        assertEquals(List.of(2, 4), moves.stream().map(StockMovementEntity::getQuantity).toList());

        assertError(ErrorCode.RECEIPT_NOT_EDITABLE, () -> service.completeStocktake(id, request));
    }

    @Test
    void differenceWithoutReasonIsRejectedAndNothingChanges() {
        Integer id = create(Map.of(shortage, 3));
        assertError(ErrorCode.ADJUSTMENT_REASON_REQUIRED, () -> service.completeStocktake(id, null));
        em.flush();
        assertEquals(5, summary(shortage).getQtyOnHand());
    }

    @Test
    void serializedDifferenceBlocksCompletion() {
        ProductEntity serialized = data.product("S", true);
        em.flush();
        Integer id = create(Map.of(serialized, 1));
        StocktakeCompleteRequest request = new StocktakeCompleteRequest();
        request.setReasons(Map.of(serialized.getId(), "thừa"));
        assertError(ErrorCode.STOCKTAKE_SERIAL_DIFFERENCE, () -> service.completeStocktake(id, request));
    }

    private Integer create(Map<ProductEntity, Integer> counts) {
        StocktakeCreationRequest request = new StocktakeCreationRequest();
        request.setWarehouseId(wh.getId());
        request.setDetails(counts.entrySet().stream().map(e -> {
            StocktakeDetailRequest d = new StocktakeDetailRequest();
            d.setProductId(e.getKey().getId());
            d.setActualQty(e.getValue());
            return d;
        }).toList());
        Integer id = service.createStocktake(request).getId();
        em.flush();
        em.clear();
        return id;
    }

    private StockSummaryEntity summary(ProductEntity p) {
        StockSummaryEntity s = em.find(StockSummaryEntity.class, new StockSummaryId(p.getId(), wh.getId()));
        em.refresh(s);
        return s;
    }

    private static void assertError(ErrorCode expected, Executable call) {
        assertEquals(expected, assertThrows(AppException.class, call).getErrorCode());
    }
}
