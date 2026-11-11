package com.tigernet.backend.printing.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VnMoneyWordsTest {
    private static String w(long v) {
        return VnMoneyWords.of(BigDecimal.valueOf(v));
    }

    @Test
    void readsCommonAmounts() {
        assertEquals("Không đồng chẵn", w(0));
        assertEquals("Năm mươi hai triệu năm trăm nghìn đồng chẵn", w(52_500_000));
        assertEquals("Hai mươi tư triệu đồng chẵn", w(24_000_000));
        assertEquals("Năm mươi chín triệu tám trăm chín mươi lăm nghìn đồng chẵn", w(59_895_000));
        assertEquals("Một trăm lẻ năm đồng chẵn", w(105));
        assertEquals("Mười lăm nghìn đồng chẵn", w(15_000));
        assertEquals("Hai mươi mốt nghìn đồng chẵn", w(21_000));
        assertEquals("Một triệu không trăm lẻ một nghìn đồng chẵn", w(1_001_000));
        assertEquals("Một tỷ không trăm hai mươi triệu đồng chẵn", w(1_020_000_000));
    }

    @Test
    void nullIsEmpty() {
        assertEquals("", VnMoneyWords.of(null));
    }
}
