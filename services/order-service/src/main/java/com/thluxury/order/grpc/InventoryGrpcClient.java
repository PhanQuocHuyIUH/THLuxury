package com.thluxury.order.grpc;

import com.thluxury.proto.common.OrderRef;
import com.thluxury.proto.inventory.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InventoryGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryGrpcClient.class);

    @GrpcClient("inventory")
    private InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    public record ReserveItem(UUID productId, UUID branchId, int quantity) {}

    public record ReserveOutcome(boolean success,
                                 String errorCode,
                                 List<ReserveItemResult> itemResults) {}

    public ReserveOutcome reserve(UUID orderId, List<ReserveItem> items, boolean strictBranch) {
        ReserveRequest.Builder req = ReserveRequest.newBuilder()
                .setOrder(OrderRef.newBuilder().setOrderId(orderId.toString()).build())
                .setStrictBranch(strictBranch);
        for (ReserveItem it : items) {
            InventoryItem.Builder ib = InventoryItem.newBuilder()
                    .setProductId(it.productId().toString())
                    .setQuantity(it.quantity());
            if (it.branchId() != null) ib.setBranchId(it.branchId().toString());
            req.addItems(ib.build());
        }
        ReserveResponse resp = stub.reserveStock(req.build());
        String errorCode = resp.getSuccess() ? null
                : (resp.hasError() ? resp.getError().getCode() : "RESERVE_FAILED");
        return new ReserveOutcome(resp.getSuccess(), errorCode, resp.getResultsList());
    }

    public void release(UUID orderId, String reason) {
        try {
            stub.releaseStock(ReleaseRequest.newBuilder()
                    .setOrder(OrderRef.newBuilder().setOrderId(orderId.toString()).build())
                    .setReason(reason == null ? "ORDER_CANCELLED" : reason)
                    .build());
        } catch (Exception e) {
            log.error("Failed to release stock for order {}", orderId, e);
        }
    }

    /** Group items by branchId thực tế từ kết quả reserve, dùng để snapshot. */
    public static Map<UUID, UUID> resolvedBranchByProduct(List<ReserveItemResult> results) {
        return results.stream()
                .filter(ReserveItemResult::getSuccess)
                .collect(Collectors.toMap(
                        r -> UUID.fromString(r.getProductId()),
                        r -> UUID.fromString(r.getBranchId()),
                        (a, b) -> a));
    }
}
