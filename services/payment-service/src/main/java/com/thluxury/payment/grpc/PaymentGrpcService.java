package com.thluxury.payment.grpc;

import com.thluxury.payment.domain.Payment;
import com.thluxury.payment.repository.PaymentRepository;
import com.thluxury.payment.service.PaymentService;
import com.thluxury.proto.common.ErrorInfo;
import com.thluxury.proto.common.Money;
import com.thluxury.proto.payment.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PaymentGrpcService.class);

    private final PaymentService payments;
    private final PaymentRepository paymentRepo;

    public PaymentGrpcService(PaymentService payments, PaymentRepository paymentRepo) {
        this.payments = payments;
        this.paymentRepo = paymentRepo;
    }

    @Override
    public void charge(ChargeRequest req, StreamObserver<ChargeResponse> obs) {
        try {
            UUID orderId = UUID.fromString(req.getOrder().getOrderId());
            Payment.Method method = mapMethod(req.getMethod());
            BigDecimal amount = BigDecimal.valueOf(req.getAmount().getAmount());
            String currency = req.getAmount().getCurrency().isEmpty() ? "VND" : req.getAmount().getCurrency();

            PaymentService.ChargeOutcome out = payments.charge(new PaymentService.ChargeInput(
                    orderId, method, amount, currency,
                    req.getCustomerId(), req.getClientToken(),
                    req.getIdempotencyKey().isEmpty() ? null : req.getIdempotencyKey()));

            ChargeResponse.Builder b = ChargeResponse.newBuilder()
                    .setPaymentId(out.payment().getId().toString())
                    .setStatus(mapStatus(out.payment().getStatus()))
                    .setGatewayRef(out.payment().getGatewayRef() == null ? "" : out.payment().getGatewayRef());
            if (out.errorCode() != null || out.errorMessage() != null) {
                b.setError(ErrorInfo.newBuilder()
                        .setCode(out.errorCode() == null ? "" : out.errorCode())
                        .setMessage(out.errorMessage() == null ? "" : out.errorMessage())
                        .build());
            }
            obs.onNext(b.build());
        } catch (Exception e) {
            log.error("Charge gRPC error", e);
            obs.onNext(ChargeResponse.newBuilder()
                    .setStatus(PaymentStatus.FAILED)
                    .setError(ErrorInfo.newBuilder().setCode("CHARGE_EXCEPTION").setMessage(e.getMessage()).build())
                    .build());
        }
        obs.onCompleted();
    }

    @Override
    public void refund(RefundRequest req, StreamObserver<RefundResponse> obs) {
        try {
            UUID paymentId = UUID.fromString(req.getPaymentId());
            BigDecimal amount = req.hasAmount() ? BigDecimal.valueOf(req.getAmount().getAmount()) : null;
            var r = payments.refund(paymentId, amount, req.getReason());
            obs.onNext(RefundResponse.newBuilder()
                    .setSuccess(r.success())
                    .setNewStatus(mapStatus(r.newStatus()))
                    .build());
        } catch (Exception e) {
            log.error("Refund gRPC error", e);
            obs.onNext(RefundResponse.newBuilder()
                    .setSuccess(false)
                    .setError(ErrorInfo.newBuilder().setCode("REFUND_EXCEPTION").setMessage(e.getMessage()).build())
                    .build());
        }
        obs.onCompleted();
    }

    @Override
    public void getPayment(GetPaymentRequest req, StreamObserver<GetPaymentResponse> obs) {
        try {
            Payment p;
            if (req.getKeyCase() == GetPaymentRequest.KeyCase.PAYMENT_ID) {
                p = paymentRepo.findById(UUID.fromString(req.getPaymentId())).orElseThrow();
            } else {
                p = paymentRepo.findByOrderId(UUID.fromString(req.getOrderId())).stream()
                        .findFirst().orElseThrow();
            }
            obs.onNext(GetPaymentResponse.newBuilder()
                    .setPaymentId(p.getId().toString())
                    .setOrderId(p.getOrderId().toString())
                    .setMethod(mapMethodOut(p.getMethod()))
                    .setStatus(mapStatus(p.getStatus()))
                    .setAmount(Money.newBuilder().setAmount(p.getAmount().longValue()).setCurrency(p.getCurrency()).build())
                    .setGatewayRef(p.getGatewayRef() == null ? "" : p.getGatewayRef())
                    .setAttemptCount(p.getAttemptCount())
                    .setCreatedAtEpoch(p.getCreatedAt().toEpochMilli())
                    .build());
        } catch (Exception e) {
            log.error("GetPayment gRPC error", e);
            obs.onNext(GetPaymentResponse.newBuilder().build());
        }
        obs.onCompleted();
    }

    private static Payment.Method mapMethod(PaymentMethod m) {
        return switch (m) {
            case COD -> Payment.Method.COD;
            case BANK_TRANSFER -> Payment.Method.BANK_TRANSFER;
            case CREDIT_CARD -> Payment.Method.CREDIT_CARD;
            default -> Payment.Method.COD;
        };
    }

    private static PaymentMethod mapMethodOut(Payment.Method m) {
        return switch (m) {
            case COD -> PaymentMethod.COD;
            case BANK_TRANSFER -> PaymentMethod.BANK_TRANSFER;
            case CREDIT_CARD -> PaymentMethod.CREDIT_CARD;
        };
    }

    private static PaymentStatus mapStatus(Payment.Status s) {
        return switch (s) {
            case PENDING -> PaymentStatus.PENDING;
            case SUCCESS -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
            case REFUNDED -> PaymentStatus.REFUNDED;
        };
    }
}
