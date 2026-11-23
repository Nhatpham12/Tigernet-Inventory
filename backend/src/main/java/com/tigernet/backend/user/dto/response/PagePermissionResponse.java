package com.tigernet.backend.user.dto.response;

import java.util.List;

/**
 * custom = false: chưa phân quyền riêng (pages rỗng), dùng quyền mặc định theo vai trò.
 * effectivePages: trang user thực sự được vào (backend dùng đúng danh sách này để chặn API); FE dựng menu theo đây.
 */
public record PagePermissionResponse(Integer userId, boolean custom, List<String> pages, List<String> effectivePages) {
}
