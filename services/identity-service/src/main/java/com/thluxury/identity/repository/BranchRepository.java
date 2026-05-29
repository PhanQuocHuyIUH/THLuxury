package com.thluxury.identity.repository;

import com.thluxury.identity.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    Optional<Branch> findByCode(String code);
    boolean existsByCode(String code);
}
