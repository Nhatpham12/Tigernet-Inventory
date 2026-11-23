package com.tigernet.bff.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyControllerTest {

    @Test
    void blocksAuthAndActuatorInAnyForm() {
        for (String path : new String[]{
                "/auth/login", "/auth/refresh", "/auth", "/AUTH/login", "//auth/login", "/Actuator/health",
                "/actuator", "/%61uth/login", "/auth%2flogin", "/auth;x/login", "/users/../auth/login",
                "\\auth/login", ""}) {
            assertTrue(ProxyController.isBlocked(path), path);
        }
    }

    @Test
    void allowsNormalApiPaths() {
        for (String path : new String[]{
                "/export-receipts", "/export-receipts/12/payments", "/users/3/page-permissions",
                "/authors", "/reports/reconciliation/print"}) {
            assertFalse(ProxyController.isBlocked(path), path);
        }
    }
}
