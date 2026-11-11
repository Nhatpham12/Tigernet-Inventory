package com.tigernet.bff.session;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.common.ApiResponse;
import com.tigernet.bff.common.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.tigernet.bff.common.Parallel.async;
import static com.tigernet.bff.common.Parallel.await;

/**
 * Thông tin một lần gọi khi mở app: người dùng, vai trò, các kho được làm việc và số cảnh báo chưa đọc
 * (header, bộ chọn kho, badge chuông trên Figma).
 */
@RestController
@RequiredArgsConstructor
public class SessionController {
    // các vai trò xem được mọi kho ("Tất cả kho") theo Frontend_Structure.md §3
    private static final Set<String> ALL_WAREHOUSE_ROLES = Set.of("ADMIN", "MANAGER", "ACCOUNTANT");

    private final BackendClient backend;

    public record WarehouseOption(Integer id, String code, String name, boolean isDefault) {
    }

    public record SessionView(Integer userId, String username, String fullName, String email, String role,
                              boolean canViewAllWarehouses, List<WarehouseOption> warehouses,
                              Integer defaultWarehouseId, long unreadAlerts, List<String> pages) {
    }

    @GetMapping("/bff/session")
    ApiResponse<SessionView> session(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        JsonNode user = backend.get("/users/me", Map.of(), authorization);
        Integer userId = Json.integer(user, "userId");
        String role = Json.text(user, "roleName");
        boolean allWarehouses = role != null && ALL_WAREHOUSE_ROLES.contains(role);

        CompletableFuture<List<WarehouseOption>> warehouses = async(() -> allWarehouses
                ? backend.getList("/warehouses", Map.of("isActive", true), authorization).stream()
                        .map(w -> new WarehouseOption(Json.integer(w, "id"), Json.text(w, "warehouseCode"),
                                Json.text(w, "warehouseName"), false))
                        .toList()
                : backend.getList("/users/" + userId + "/warehouses", Map.of(), authorization).stream()
                        .map(w -> new WarehouseOption(Json.integer(w, "warehouseId"), Json.text(w, "warehouseCode"),
                                Json.text(w, "warehouseName"), Boolean.TRUE.equals(w.path("isDefault").asBoolean())))
                        .toList());
        CompletableFuture<JsonNode> unread = async(() -> backend.get("/alerts/unread-count", Map.of(), authorization));
        // trang được xem do Quản trị phân quyền riêng; null = dùng quyền mặc định theo vai trò
        CompletableFuture<JsonNode> permissions = async(() -> backend.get("/users/me/page-permissions", Map.of(), authorization));

        List<WarehouseOption> options = await(warehouses);
        Integer defaultWarehouseId = options.stream().filter(WarehouseOption::isDefault).map(WarehouseOption::id)
                .findFirst()
                // QL/QT/KT mặc định "Tất cả kho" (null); TK/BH lấy kho đầu tiên được phân công
                .orElse(allWarehouses || options.isEmpty() ? null : options.getFirst().id());
        JsonNode unreadNode = await(unread);
        JsonNode permissionNode = await(permissions);
        List<String> pages = null;
        if (permissionNode != null && permissionNode.path("custom").asBoolean(false)) {
            pages = new ArrayList<>();
            for (JsonNode page : permissionNode.path("pages")) {
                pages.add(page.asText());
            }
        }

        return ApiResponse.success(new SessionView(userId, Json.text(user, "username"), Json.text(user, "fullName"),
                Json.text(user, "email"), role, allWarehouses, options, defaultWarehouseId,
                unreadNode == null ? 0 : unreadNode.asLong(), pages));
    }
}
