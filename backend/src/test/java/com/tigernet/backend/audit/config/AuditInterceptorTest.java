package com.tigernet.backend.audit.config;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.common.response.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditInterceptorTest {
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuditInterceptor interceptor = new AuditInterceptor(auditLogService, JsonMapper.builder().build());
    private final AuditResponseCapture capture = new AuditResponseCapture();

    record Created(Integer id, String receiptCode, String password) {}

    @BeforeEach
    void login() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("demo").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createLogsNewIdAndSummaryFromResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/import-receipts");
        capture.beforeBodyWrite(ApiResponse.success(new Created(12, "PN-KHO1-2026-0005", "x")), null, null, null,
                new ServletServerHttpRequest(request), null);

        interceptor.afterCompletion(request, new MockHttpServletResponse(), null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> detail = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).logDocument(eq("CREATE"), eq("import-receipts"), eq(12), eq("PN-KHO1-2026-0005"),
                detail.capture());
        assertThat(detail.getValue()).containsEntry("method", "POST").containsEntry("path", "/import-receipts");
        assertThat(detail.getValue().get("summary"))
                .isEqualTo(Map.of("id", 12, "receiptCode", "PN-KHO1-2026-0005")); // không có password
    }

    @Test
    void readRequestIsNotCaptured() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/import-receipts/1");
        capture.beforeBodyWrite(ApiResponse.success(new Created(1, "PN", null)), null, null, null,
                new ServletServerHttpRequest(request), null);

        assertThat(request.getAttribute(AuditResponseCapture.RESULT_ATTRIBUTE)).isNull();
    }

    @Test
    void summaryFailureStillWritesLog() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/products/7");
        Object broken = new Object() {
            public String getName() { throw new IllegalStateException("lazy"); }
        };
        request.setAttribute(AuditResponseCapture.RESULT_ATTRIBUTE, broken);

        interceptor.afterCompletion(request, new MockHttpServletResponse(), null, null);

        verify(auditLogService).logDocument(eq("UPDATE"), anyString(), eq(7), isNull(), any());
    }
}
