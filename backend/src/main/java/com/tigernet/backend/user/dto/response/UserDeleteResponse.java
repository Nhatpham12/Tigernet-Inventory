package com.tigernet.backend.user.dto.response;

// deleted = true: đã xóa hẳn; false: tài khoản đã có lịch sử nên chỉ bị khóa (xóa mềm, BACKLOG QT-01)
public record UserDeleteResponse(Integer userId, boolean deleted) {
}
