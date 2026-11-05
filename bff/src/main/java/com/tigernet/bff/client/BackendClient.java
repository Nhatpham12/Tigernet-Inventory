package com.tigernet.bff.client;

import com.tigernet.bff.config.BffProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gọi API backend. Mọi lời gọi đều mang access token của người dùng (header Authorization)
 * nên backend vẫn là nơi duy nhất kiểm tra quyền; BFF không tự cấp quyền.
 */
@Component
public class BackendClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BackendClient(BffProperties properties, ObjectMapper objectMapper) {
        // java.net.http.HttpClient: đọc được body lỗi 401 (HttpURLConnection trả body rỗng ở 401)
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.backend().connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.backend().readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.backend().baseUrl())
                .requestFactory(factory)
                .build();
        this.objectMapper = objectMapper;
    }

    /** Kết quả thô của một lời gọi, dùng cho proxy. */
    public record RawResponse(int status, MediaType contentType, HttpHeaders headers, byte[] body) {
    }

    public RawResponse exchange(HttpMethod method, String path, MultiValueMap<String, String> query,
                                String authorization, MediaType contentType, byte[] body) {
        String uri = UriComponentsBuilder.fromPath(path).queryParams(query).build().encode().toUriString();
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(uri);
            if (authorization != null) {
                spec.header(HttpHeaders.AUTHORIZATION, authorization);
            }
            if (body != null && body.length > 0) {
                spec.contentType(contentType != null ? contentType : MediaType.APPLICATION_JSON).body(body);
            }
            return spec.exchange((request, response) -> new RawResponse(
                    response.getStatusCode().value(),
                    response.getHeaders().getContentType(),
                    response.getHeaders(),
                    response.getBody().readAllBytes()), true);
        } catch (ResourceAccessException e) {
            throw unavailable();
        }
    }

    /** GET và trả về trường "result" của ApiResponse; lỗi từ backend ném BackendException. */
    public JsonNode get(String path, Map<String, ?> query, String authorization) {
        return call(HttpMethod.GET, path, query, authorization, null);
    }

    public JsonNode post(String path, Object body, String authorization) {
        return call(HttpMethod.POST, path, Map.of(), authorization, body);
    }

    /** GET một danh sách; null/không phải mảng thì trả mảng rỗng. */
    public List<JsonNode> getList(String path, Map<String, ?> query, String authorization) {
        JsonNode result = get(path, query, authorization);
        if (result instanceof ArrayNode array) {
            return array.valueStream().toList();
        }
        return List.of();
    }

    private JsonNode call(HttpMethod method, String path, Map<String, ?> query, String authorization, Object body) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        query.forEach((key, value) -> {
            if (value != null && !value.toString().isBlank()) {
                params.add(key, value.toString());
            }
        });
        byte[] bytes = body == null ? null : objectMapper.writeValueAsBytes(body);
        RawResponse response = exchange(method, path, params, authorization, MediaType.APPLICATION_JSON, bytes);
        if (response.status() >= 400) {
            throw new BackendException(response.status(), response.body(),
                    "Backend " + method + " " + path + " -> " + response.status());
        }
        if (response.body().length == 0) {
            return null;
        }
        return objectMapper.readTree(response.body()).get("result");
    }

    private BackendException unavailable() {
        String json = "{\"code\":9503,\"message\":\"Backend is unavailable\"}";
        return new BackendException(503, json.getBytes(StandardCharsets.UTF_8), "Backend is unavailable");
    }
}
