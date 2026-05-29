package com.thluxury.payment.repository;

import com.thluxury.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByGatewayRef(String gatewayRef);
}
