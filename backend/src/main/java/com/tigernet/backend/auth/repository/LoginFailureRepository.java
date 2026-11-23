package com.tigernet.backend.auth.repository;

import com.tigernet.backend.auth.entity.LoginFailureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginFailureRepository extends JpaRepository<LoginFailureEntity, LoginFailureEntity.Key> {
    // Quản trị đặt lại mật khẩu / xoá user: mở mọi khoá của user đó
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LoginFailureEntity f where f.id.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    // dọn dòng cũ để bảng không phình (gọi khi ghi lần sai mới)
    @Modifying
    @Query("delete from LoginFailureEntity f where f.updatedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
