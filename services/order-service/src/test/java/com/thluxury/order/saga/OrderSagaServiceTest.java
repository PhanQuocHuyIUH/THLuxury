package com.thluxury.order.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thluxury.order.domain.DeliveryType;
import com.thluxury.order.domain.OrderEntity;
import com.thluxury.order.domain.OrderStatus;
import com.thluxury.order.grpc.InventoryGrpcClient;
import com.thluxury.order.grpc.PaymentGrpcClient;
import com.thluxury.order.messaging.OrderEventPublisher;
import com.thluxury.order.repository.OrderEventRepository;
import com.thluxury.order.repository.OrderRepository;
import com.thluxury.order.repository.TaxConfigRepository;
import com.thluxury.order.service.CartService;
import com.thluxury.order.service.CatalogClient;
import com.thluxury.order.service.VoucherService;
import com.thluxury.order.web.dto.CheckoutDtos.CheckoutRequest;
import com.thluxury.order.web.dto.CheckoutDtos.CustomerInput;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Saga unit test — happy path + compensate paths.
 * Tất cả collaborator được mock; không cần Spring context.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderSagaServiceTest {

    @Mock OrderRepository orders;
    @Mock OrderEventRepository events;
    @Mock TaxConfigRepository taxConfigs;
    @Mock CartService carts;
    @Mock CatalogClient catalog;
    @Mock VoucherService vouchers;
    @Mock InventoryGrpcClient inventory;
    @Mock PaymentGrpcClient payment;
    @Mock OrderEventPublisher publisher;

    private final ObjectMapper json = new ObjectMapper();
    private OrderSagaService saga;

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        saga = new OrderSagaService(
                orders, events, taxConfigs, carts, catalog, vouchers,
                inventory, payment, json, publisher, new SimpleMeterRegistry(), true);

        // Catalog stub: 1 product, giaHienTai = 1.000.000
        ObjectNode product = json.createObjectNode();
        product.put("maSp", 1001L);
        product.put("tenSp", "Test Ring");
        product.put("giaHienTai", 1_000_000);
        product.put("giaBanDau", 1_200_000);
        when(catalog.getProduct(productId)).thenReturn(product);

        // Cart stub: 1 sản phẩm, qty=1
        when(carts.get(userId)).thenReturn(Map.of(productId, 1));

        // Order persist: trả về chính object đã setId
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });

        // Tax config: empty → mặc định VAT 10%
        when(taxConfigs.findByCode("VAT")).thenReturn(Optional.empty());

        // Voucher: không áp dụng (null)
        when(vouchers.applyVoucher(null, BigDecimal.valueOf(1_000_000))).thenReturn(null);
    }

    private CheckoutRequest pickupRequest() {
        return new CheckoutRequest(DeliveryType.STORE_PICKUP, branchId, null,
                null, "COD", null);
    }

    private CustomerInput customer() {
        return new CustomerInput("Test User", "0900000000", "test@example.com");
    }

    @Test
    void happyPath_COD_to_CONFIRMED() {
        // Reserve OK
        when(inventory.reserve(any(UUID.class), any(), eq(true)))
                .thenReturn(new InventoryGrpcClient.ReserveOutcome(true, null, List.of()));
        // COD → Payment trả PENDING → Order = CONFIRMED (chưa thu tiền), KHÔNG phải PAID.
        when(payment.charge(any(), anyString(), eq("COD"), any(), anyString(), anyString()))
                .thenReturn(new PaymentGrpcClient.ChargeOutcome("pay-1", "PENDING", "ref-1", null, null));

        OrderEntity result = saga.checkout(userId, customer(), pickupRequest());

        assertThat(result.getCurrentStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getMaDh()).startsWith("DH");
        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1_100_000)); // 1tr + 10% VAT
        verify(publisher).publishCreated(any());
        verify(publisher).publishPaid(any()); // vẫn publish để Inventory commit kho
        verify(publisher, never()).publishFailed(any(), anyString());
        verify(inventory, never()).release(any(), anyString());
        verify(carts).clear(userId);
    }

    @Test
    void onlinePayment_SUCCESS_to_PAID() {
        when(inventory.reserve(any(UUID.class), any(), eq(true)))
                .thenReturn(new InventoryGrpcClient.ReserveOutcome(true, null, List.of()));
        // Thanh toán online (CREDIT_CARD) capture thành công → PAID.
        CheckoutRequest cardReq = new CheckoutRequest(DeliveryType.STORE_PICKUP, branchId, null,
                null, "CREDIT_CARD", null);
        when(payment.charge(any(), anyString(), eq("CREDIT_CARD"), any(), anyString(), anyString()))
                .thenReturn(new PaymentGrpcClient.ChargeOutcome("pay-2", "SUCCESS", "ref-2", null, null));

        OrderEntity result = saga.checkout(userId, customer(), cardReq);

        assertThat(result.getCurrentStatus()).isEqualTo(OrderStatus.PAID);
        verify(publisher).publishPaid(any());
        verify(publisher, never()).publishFailed(any(), anyString());
    }

    @Test
    void paymentFails_compensates_inventory_and_marks_FAILED() {
        when(inventory.reserve(any(UUID.class), any(), eq(true)))
                .thenReturn(new InventoryGrpcClient.ReserveOutcome(true, null, List.of()));
        when(payment.charge(any(), anyString(), eq("COD"), any(), anyString(), anyString()))
                .thenReturn(new PaymentGrpcClient.ChargeOutcome(null, "FAILED", null,
                        "GATEWAY_DECLINED", "Declined"));

        OrderEntity result = saga.checkout(userId, customer(), pickupRequest());

        assertThat(result.getCurrentStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(result.getFailureReason()).startsWith("PAYMENT_FAILED");
        verify(inventory).release(any(), eq("PAYMENT_FAILED"));
        verify(publisher).publishFailed(any(), eq("PAYMENT_FAILED"));
        verify(publisher, never()).publishPaid(any());
    }

    @Test
    void inventoryReserveFails_failsOrder_immediately() {
        when(inventory.reserve(any(UUID.class), any(), eq(true)))
                .thenReturn(new InventoryGrpcClient.ReserveOutcome(false, "INSUFFICIENT_STOCK", List.of()));

        assertThatThrownBy(() -> saga.checkout(userId, customer(), pickupRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INSUFFICIENT_STOCK");

        verify(publisher).publishCreated(any());
        verify(publisher).publishFailed(any(), eq("INVENTORY_RESERVE_FAILED"));
        verify(payment, never()).charge(any(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void inventoryGrpcThrows_failsOrder_without_payment_call() {
        when(inventory.reserve(any(UUID.class), any(), eq(true)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> saga.checkout(userId, customer(), pickupRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVENTORY_UNAVAILABLE");

        verify(publisher).publishFailed(any(), eq("INVENTORY_UNAVAILABLE"));
        verify(payment, never()).charge(any(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void emptyCart_throws_without_any_side_effects() {
        when(carts.get(userId)).thenReturn(Map.of());

        assertThatThrownBy(() -> saga.checkout(userId, customer(), pickupRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CART_EMPTY");

        verify(orders, never()).save(any());
        verify(publisher, never()).publishCreated(any());
    }

    @Test
    void storePickup_without_branchId_throws() {
        CheckoutRequest req = new CheckoutRequest(DeliveryType.STORE_PICKUP, null, null,
                null, "COD", null);

        assertThatThrownBy(() -> saga.checkout(userId, customer(), req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BRANCH_ID_REQUIRED_FOR_PICKUP");
    }

    @Test
    void emits_correct_event_sequence_on_happy_path() {
        when(inventory.reserve(any(UUID.class), any(), eq(true)))
                .thenReturn(new InventoryGrpcClient.ReserveOutcome(true, null, List.of()));
        when(payment.charge(any(), anyString(), eq("COD"), any(), anyString(), anyString()))
                .thenReturn(new PaymentGrpcClient.ChargeOutcome("pay-1", "PENDING", "ref-1", null, null));

        saga.checkout(userId, customer(), pickupRequest());

        ArgumentCaptor<com.thluxury.order.domain.OrderEvent> captor =
                ArgumentCaptor.forClass(com.thluxury.order.domain.OrderEvent.class);
        verify(events, times(5)).save(captor.capture());
        List<String> types = captor.getAllValues().stream()
                .map(com.thluxury.order.domain.OrderEvent::getEventType)
                .toList();
        assertThat(types).containsExactly(
                "ORDER_CREATED",
                "INVENTORY_RESERVED",
                "PRICING_APPLIED",
                "PAYMENT_INITIATED",
                "ORDER_CONFIRMED"); // COD → ORDER_CONFIRMED (trước đây là PAYMENT_SUCCESS)
    }
}
