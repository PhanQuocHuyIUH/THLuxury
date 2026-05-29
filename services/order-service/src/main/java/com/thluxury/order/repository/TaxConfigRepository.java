package com.thluxury.order.repository;

import com.thluxury.order.domain.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxConfigRepository extends JpaRepository<TaxConfig, UUID> {
    Optional<TaxConfig> findByCode(String code);
}
