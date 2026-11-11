package com.tigernet.backend.printing.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StocktakeAdjustmentDotLinesTest {

    @Test
    void blankReasonKeepsAllDottedLines() {
        assertEquals(4, StocktakePrintService.dotLines(null));
        assertEquals(4, StocktakePrintService.dotLines("  "));
    }

    @Test
    void reasonUsesUpLinesButKeepsAtLeastOne() {
        assertEquals(3, StocktakePrintService.dotLines("Thừa khi kiểm kê"));
        assertEquals(2, StocktakePrintService.dotLines("x".repeat(86)));
        assertEquals(1, StocktakePrintService.dotLines("x".repeat(500)));
    }
}
