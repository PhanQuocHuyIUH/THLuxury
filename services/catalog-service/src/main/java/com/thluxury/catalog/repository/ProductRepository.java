package com.thluxury.catalog.repository;

import com.thluxury.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByMaSp(Long maSp);

    @Query("SELECT DISTINCT p.loaiSp FROM Product p WHERE p.status = com.thluxury.catalog.domain.ProductStatus.ACTIVE ORDER BY p.loaiSp")
    List<String> findDistinctLoaiSp();
}
