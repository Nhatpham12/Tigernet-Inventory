package com.tigernet.backend.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API chỉ cho các vai trò này (tên Role_Name, vd "MANAGER"); Quản trị luôn được.
 * Dùng cho việc cấp quản lý không phân quyền theo trang được (duyệt, chốt, cấu hình) và màn Quản trị.
 * Đi cùng @RequiresPage thì phải thoả cả hai.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    String[] value();
}
