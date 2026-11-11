package com.tigernet.backend.user.dto.response;

import java.util.List;

// custom = false: chưa phân quyền riêng, FE dùng quyền mặc định theo vai trò (pages rỗng)
public record PagePermissionResponse(Integer userId, boolean custom, List<String> pages) {
}
