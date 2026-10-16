package com.tigernet.backend.user.repository;

import com.tigernet.backend.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity,Integer> {
    boolean existsByUsername (String username);
    Optional<UserEntity> findByUsername(String username);
}
