package com.tigernet.backend.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API chỉ gọi được khi user được vào ít nhất một trong các trang này (page key "/app/..." như menu FE).
 * Quyền trang = danh sách phân quyền riêng của user nếu có, không thì mặc định theo vai trò
 * (xem PageAccessPolicy). Quản trị vào mọi trang. Đặt ở method hoặc class (method ưu tiên).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPage {
    String[] value();
}
