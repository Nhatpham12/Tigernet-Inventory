package com.tigernet.backend.printing.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrintResponsesTest {

    @Test
    void fileNameUsesBusinessNameAndTimeOfTheTransaction() {
        assertEquals("Phieu_nhap_kho_20261103_0930",
                PrintResponses.fileName("Phieu_nhap_kho", LocalDateTime.of(2026, 11, 3, 9, 30, 15)));
        assertEquals("Bien_ban_doi_chieu_cong_no_20261031",
                PrintResponses.fileName("Bien_ban_doi_chieu_cong_no", LocalDate.of(2026, 10, 31)));
    }

    @Test
    void fileNameFallsBackToNowWithoutTime() {
        assertTrue(PrintResponses.fileName("Phieu_thu", null).matches("Phieu_thu_\\d{8}_\\d{4}"));
    }
}
