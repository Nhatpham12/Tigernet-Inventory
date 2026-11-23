package com.tigernet.backend.user.repository;

import com.tigernet.backend.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity,Integer> {
    boolean existsByUsername (String username);
    Optional<UserEntity> findByUsername(String username);

    // kiểm tra access token mỗi request: rỗng nếu user không tồn tại hoặc đã bị khoá
    @Query("select u.tokenVersion from UserEntity u where u.userId = :userId and u.isActive = true")
    Optional<Integer> findActiveTokenVersion(@Param("userId") Integer userId);
}
