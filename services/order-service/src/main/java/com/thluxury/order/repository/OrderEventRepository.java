package com.thluxury.order.repository;

import com.thluxury.order.domain.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {
    List<OrderEvent> findByOrderIdOrderBySequenceNoAsc(UUID orderId);
    long countByOrderId(UUID orderId);
}
