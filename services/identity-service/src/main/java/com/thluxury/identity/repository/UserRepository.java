package com.thluxury.identity.repository;

import com.thluxury.identity.domain.User;
import com.thluxury.identity.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND (:branchId IS NULL OR u.branchId = :branchId)")
    Page<User> search(UserRole role, UUID branchId, Pageable pageable);
}
