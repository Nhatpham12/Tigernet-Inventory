package com.tigernet.bff.dashboard;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.client.BackendException;
import com.tigernet.bff.view.ProductStockService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Nhân viên không được xem một loại phiếu (backend 403) thì dashboard vẫn hiện phần còn lại. */
class DashboardControllerTest {
    BackendClient backend = mock(BackendClient.class);
    ProductStockService products = mock(ProductStockService.class);
    DashboardController controller = new DashboardController(backend, products);

    @Test
    void forbiddenListIsSkipped() {
        when(products.load(any(), any(), anyString())).thenReturn(List.of());
        when(backend.getList(eq("/import-receipts"), any(), anyString()))
                .thenThrow(new BackendException(403, new byte[0], "forbidden"));
        when(backend.getList(eq("/export-receipts"), any(), anyString())).thenReturn(List.of());
        when(backend.getList(eq("/rental-receipts"), any(), anyString())).thenReturn(List.of());

        var view = controller.dashboard("Bearer t", null).result();
        assertEquals(0, view.attention().pendingApprovalCount());
    }

    @Test
    void otherBackendErrorsStillFail() {
        when(products.load(any(), any(), anyString())).thenReturn(List.of());
        when(backend.getList(anyString(), any(), anyString()))
                .thenThrow(new BackendException(503, new byte[0], "down"));
        assertThrows(RuntimeException.class, () -> controller.dashboard("Bearer t", null));
    }
}
