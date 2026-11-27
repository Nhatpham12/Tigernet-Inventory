package com.tigernet.backend.printing.service;

import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.entity.SupplierEntity;
import com.tigernet.backend.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Biên bản đối chiếu công nợ render được cho khách (đối tác là bên mua) và cho NCC (đối tác là bên bán). */
@SpringBootTest
@Transactional
class ReconciliationPrintIntegrationTest {
    @Autowired ReconciliationPrintService service;
    @Autowired EntityManager em;

    @Test
    void rendersForCustomerAndSupplier() {
        TestData data = TestData.of(em, "rec_it_");
        CustomerEntity customer = data.customer("C");
        SupplierEntity supplier = data.supplier("N");
        em.flush();

        String forCustomer = render(customer.getParty().getId());
        assertTrue(forCustomer.contains("Bên A (Bên mua): <span>" + data.tag() + "C"), forCustomer);

        String forSupplier = render(supplier.getParty().getId());
        assertTrue(forSupplier.contains("Bên B (Bên bán): <span>" + data.tag() + "N"), forSupplier);
    }

    private String render(Integer partyId) {
        ResponseEntity<byte[]> response = service.print(partyId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), "html", false);
        assertEquals(200, response.getStatusCode().value());
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }
}
