package com.tigernet.backend.payment.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.inbound.service.ImportReceiptService;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import com.tigernet.backend.outbound.service.ExportReceiptService;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.service.PartyService;
import com.tigernet.backend.payment.dto.request.PaymentCancelRequest;
import com.tigernet.backend.payment.dto.request.PaymentCreationRequest;
import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import com.tigernet.backend.payment.mapper.PaymentMapper;
import com.tigernet.backend.payment.dto.response.PaymentResponse;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentManagementServiceTest {
    @Mock PaymentService paymentService;
    @Mock PaymentMapper paymentMapper;
    @Mock PartyService partyService;
    @Mock ExportReceiptService exportReceiptService;
    @Mock ImportReceiptService importReceiptService;
    @Mock ExportReceiptRepository exportReceiptRepository;
    @Mock ImportReceiptRepository importReceiptRepository;
    @Mock AuditLogService auditLogService;
    @InjectMocks PaymentManagementService service;

    PartyEntity customerParty;
    ExportReceiptEntity export;

    @BeforeEach
    void setUp() {
        customerParty = new PartyEntity();
        customerParty.setId(10);
        CustomerEntity customer = new CustomerEntity();
        customer.setParty(customerParty);
        WarehouseEntity wh = new WarehouseEntity();
        wh.setWarehouseCode("MAIN");
        export = new ExportReceiptEntity();
        export.setId(5);
        export.setReceiptCode("PX-MAIN-2026-0005");
        export.setCustomer(customer);
        export.setWarehouse(wh);
        export.setStatus(ExportStatus.EXPORTED);
    }

    private PaymentCreationRequest request(PaymentDirection dir, Integer partyId, PaymentReferenceType type) {
        return PaymentCreationRequest.builder().direction(dir).partyId(partyId)
                .amount(new BigDecimal("250000")).referenceType(type).referenceId(type == null ? null : 5).build();
    }

    @Test
    void exportPaymentAddsToPaidAndUsesReceiptWarehouseForCode() {
        when(partyService.findParty(10)).thenReturn(customerParty);
        when(exportReceiptService.findReceipt(5)).thenReturn(export);
        PaymentEntity saved = new PaymentEntity();
        saved.setReferenceType(PaymentReferenceType.Export_Receipts);
        saved.setReferenceId(5);
        when(paymentService.record(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(saved);
        when(paymentMapper.toResponse(saved)).thenReturn(new PaymentResponse());
        when(exportReceiptRepository.findById(5)).thenReturn(Optional.of(export));

        PaymentResponse r = service.createPayment(request(PaymentDirection.IN, 10, PaymentReferenceType.Export_Receipts));

        verify(exportReceiptService).adjustPaid(export, new BigDecimal("250000.00"));
        verify(paymentService).record(eq(PaymentDirection.IN), eq(customerParty), eq(PaymentReferenceType.Export_Receipts),
                eq(5), any(), any(), any(), eq("MAIN"), any());
        assertEquals("PX-MAIN-2026-0005", r.getReferenceCode());
    }

    @Test
    void wrongDirectionPartyOrMissingReferenceIsRejected() {
        assertEquals(ErrorCode.PAYMENT_REFERENCE_INVALID, assertThrows(AppException.class,
                () -> service.createPayment(request(PaymentDirection.IN, 10, null))).getErrorCode());

        when(partyService.findParty(10)).thenReturn(customerParty);
        when(exportReceiptService.findReceipt(5)).thenReturn(export);
        assertEquals(ErrorCode.PAYMENT_REFERENCE_INVALID, assertThrows(AppException.class,
                () -> service.createPayment(request(PaymentDirection.OUT, 10, PaymentReferenceType.Export_Receipts)))
                .getErrorCode());
        verify(exportReceiptService, never()).adjustPaid(any(), any());
    }

    @Test
    void cancelSubtractsPaidAndMarksCancelled() {
        PaymentEntity p = new PaymentEntity();
        p.setId(77);
        p.setReferenceType(PaymentReferenceType.Export_Receipts);
        p.setReferenceId(5);
        p.setAmount(new BigDecimal("250000.00"));
        p.setStatus(PaymentEntryStatus.COMPLETED);
        when(paymentService.findPayment(77)).thenReturn(p);
        when(exportReceiptRepository.findById(5)).thenReturn(Optional.of(export));
        when(paymentMapper.toResponse(p)).thenReturn(new PaymentResponse());

        service.cancelPayment(77, new PaymentCancelRequest("nhập nhầm"));

        verify(exportReceiptService).adjustPaid(export, new BigDecimal("-250000.00"));
        assertEquals(PaymentEntryStatus.CANCELLED, p.getStatus());
        assertEquals(ErrorCode.PAYMENT_ALREADY_CANCELLED, assertThrows(AppException.class,
                () -> service.cancelPayment(77, null)).getErrorCode());
    }

    @Test
    void importCancelSubtractsPaidOnImportReceipt() {
        ImportReceiptEntity imp = new ImportReceiptEntity();
        imp.setStatus(ImportStatus.CONFIRMED);
        PaymentEntity p = new PaymentEntity();
        p.setReferenceType(PaymentReferenceType.Import_Receipts);
        p.setReferenceId(8);
        p.setAmount(new BigDecimal("100.00"));
        p.setStatus(PaymentEntryStatus.COMPLETED);
        when(paymentService.findPayment(1)).thenReturn(p);
        when(importReceiptRepository.findById(8)).thenReturn(Optional.of(imp));
        when(paymentMapper.toResponse(p)).thenReturn(new PaymentResponse());

        service.cancelPayment(1, null);
        verify(importReceiptService).adjustPaid(imp, new BigDecimal("-100.00"));
    }
}
