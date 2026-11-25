package com.tigernet.backend.audit.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSummaryTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    record Line(Integer id, String productName, String serial, Integer quantity, BigDecimal priceIn) {}

    record Receipt(Integer id, String receiptCode, String warehouseName, String sourcePartyName,
                   BigDecimal totalAmount, String status, String createdByName, String phone, String address,
                   String note, List<Line> details) {}

    @Test
    void keepsWhitelistedFieldsAndSummarizesLines() {
        Receipt receipt = new Receipt(12, "PN-KHO1-2026-0005", "Kho 1", "NCC A", new BigDecimal("12500000"),
                "PENDING", "Admin", "0909000000", "1 Đường A", "giao gấp",
                List.of(new Line(1, "Router X", "SN1", 1, BigDecimal.TEN), new Line(2, "Cáp", null, 5, BigDecimal.ONE)));

        Map<String, Object> summary = AuditSummary.of(receipt, mapper);

        assertThat(summary).containsEntry("id", 12)
                .containsEntry("receiptCode", "PN-KHO1-2026-0005")
                .containsEntry("warehouseName", "Kho 1")
                .containsEntry("sourcePartyName", "NCC A")
                .containsEntry("status", "PENDING")
                .containsEntry("note", "giao gấp")
                .containsEntry("itemCount", 2)
                .containsKey("totalAmount")
                // người thao tác đã có User_ID; SĐT/địa chỉ là dữ liệu cá nhân
                .doesNotContainKeys("createdByName", "phone", "address", "details");
        assertThat(summary.get("items")).isEqualTo(List.of("Router X [SN1] × 1", "Cáp × 5"));
    }

    @Test
    void dropsSensitiveKeysEvenIfWhitelisted() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", 3);
        result.put("username", "demo");
        result.put("password", "x");
        result.put("accessTokenCode", "abc");
        result.put("mfaSecretName", "s");
        result.put("recoveryCode", "r");

        assertThat(AuditSummary.of(result, mapper)).containsOnlyKeys("id", "username");
    }

    @Test
    void listResultGivesCountAndNullGivesNothing() {
        assertThat(AuditSummary.of(List.of(1, 2, 3), mapper)).isEqualTo(Map.of("count", 3));
        assertThat(AuditSummary.of(null, mapper)).isNull();
        assertThat(AuditSummary.of("ok", mapper)).isNull();
        assertThat(AuditSummary.of(Map.of("phone", "0909"), mapper)).isNull();
    }

    @Test
    void capsItemsAndLongText() {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            lines.add(new Line(i, "SP " + i, null, 1, null));
        }
        Receipt receipt = new Receipt(1, "PX-1", null, null, null, null, null, null, null, "a".repeat(500), lines);

        Map<String, Object> summary = AuditSummary.of(receipt, mapper);

        assertThat(summary).containsEntry("itemCount", 30);
        assertThat((List<?>) summary.get("items")).hasSize(AuditSummary.MAX_ITEMS);
        assertThat((String) summary.get("note")).hasSize(AuditSummary.MAX_TEXT + 1);
    }

    @Test
    void dropsItemListWhenTooLong() {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lines.add(new Line(i, "SP", "s".repeat(500), 1, null)); // serial dài bất thường
        }
        Receipt receipt = new Receipt(1, "PX-1", null, null, null, null, null, null, null, null, lines);

        Map<String, Object> summary = AuditSummary.of(receipt, mapper);

        assertThat(summary).containsEntry("itemCount", 10).doesNotContainKey("items");
        assertThat(mapper.writeValueAsString(summary).length()).isLessThanOrEqualTo(AuditSummary.MAX_JSON);
    }
}
