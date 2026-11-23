package com.tigernet.backend.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ghi rõ API này không kiểm tra quyền trang/vai trò (vẫn cần đăng nhập theo SecurityConfig, trừ /auth/**).
 * Dùng cho dữ liệu tra cứu dùng chung (sản phẩm, kho, khách hàng cho dropdown...) và thông tin của chính mình.
 * API không mang annotation nào bị chặn (AccessInterceptor mặc định từ chối).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoPageCheck {
    /** lý do (để người review thấy vì sao API này mở cho mọi user đã đăng nhập) */
    String value();
}
