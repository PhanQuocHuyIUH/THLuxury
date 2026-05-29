package com.thluxury.order.repository;

import com.thluxury.order.domain.OrderEntity;
import com.thluxury.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByMaDh(String maDh);

    Page<OrderEntity> findByCustomerId(UUID customerId, Pageable pageable);
    Page<OrderEntity> findByCustomerIdAndCurrentStatus(UUID customerId, OrderStatus status, Pageable pageable);

    Page<OrderEntity> findByBranchId(UUID branchId, Pageable pageable);
    Page<OrderEntity> findByBranchIdAndCurrentStatus(UUID branchId, OrderStatus status, Pageable pageable);
    Page<OrderEntity> findByBranchIdAndCurrentStatusIn(UUID branchId, List<OrderStatus> statuses, Pageable pageable);

    Page<OrderEntity> findByCurrentStatus(OrderStatus status, Pageable pageable);
    Page<OrderEntity> findByCurrentStatusIn(List<OrderStatus> statuses, Pageable pageable);
}
