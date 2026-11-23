package com.tigernet.bff.auth;

import com.tigernet.bff.config.BffProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieAuthFilterTest {
    BffProperties properties = new BffProperties(null,
            new BffProperties.Cors(List.of("https://tigernet.example.com", "http://localhost:*")),
            new BffProperties.AuthCookie(true, Duration.ofDays(7)));
    AuthCookies cookies = new AuthCookies(properties);
    CookieAuthFilter filter = new CookieAuthFilter(cookies, properties);

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/bff/api/export-receipts");
        request.setServerName("tigernet.example.com");
        return request;
    }

    private HttpServletRequest run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return (HttpServletRequest) chain.getRequest();
    }

    @Test
    void writeWithoutCsrfHeaderIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(run(request("POST"), response));
        assertEquals(403, response.getStatus());
    }

    @Test
    void writeFromForeignOriginIsRejected() throws Exception {
        MockHttpServletRequest request = request("POST");
        request.addHeader(CookieAuthFilter.CSRF_HEADER, "1");
        request.addHeader(HttpHeaders.ORIGIN, "https://evil.example.net");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(run(request, response));
        assertEquals(403, response.getStatus());
    }

    @Test
    void sameHostOrAllowedOriginPasses() throws Exception {
        for (String origin : new String[]{"https://tigernet.example.com", "http://localhost:5173"}) {
            MockHttpServletRequest request = request("PUT");
            request.addHeader(CookieAuthFilter.CSRF_HEADER, "1");
            request.addHeader(HttpHeaders.ORIGIN, origin);
            assertTrue(run(request, new MockHttpServletResponse()) != null, origin);
        }
    }

    @Test
    void getNeedsNoCsrfHeaderAndCookieBecomesBearer() throws Exception {
        MockHttpServletRequest request = request("GET");
        request.setCookies(new Cookie(cookies.accessName, "abc.def.ghi"));
        HttpServletRequest passed = run(request, new MockHttpServletResponse());
        assertEquals("Bearer abc.def.ghi", passed.getHeader(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void authCookiesAreHttpOnlySecureStrict() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        cookies.write(response, "access", 3600, "refresh", false);
        List<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(setCookies.stream().allMatch(c -> c.contains("HttpOnly") && c.contains("Secure")
                && c.contains("SameSite=Strict")), setCookies.toString());
        String refresh = setCookies.stream().filter(c -> c.startsWith("__Secure-tn_rt=")).findFirst().orElseThrow();
        assertTrue(refresh.contains("Path=/bff/auth") && !refresh.contains("Max-Age"), refresh); // cookie phiên
        assertTrue(setCookies.stream().anyMatch(c -> c.startsWith("__Host-tn_at=") && c.contains("Path=/")));
    }
}
