package com.thluxury.inventory.service;

import com.thluxury.proto.common.ErrorInfo;
import com.thluxury.proto.inventory.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(InventoryGrpcService.class);

    private final InventoryService inventoryService;

    public InventoryGrpcService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void reserveStock(ReserveRequest request, StreamObserver<ReserveResponse> responseObserver) {
        log.info("Received reserve request for order: {}", request.getOrder().getOrderId());
        boolean success = true;
        ReserveResponse.Builder responseBuilder = ReserveResponse.newBuilder();

        for (InventoryItem item : request.getItemsList()) {
            ReserveItemResult.Builder itemResult = ReserveItemResult.newBuilder()
                    .setProductId(item.getProductId())
                    .setReserved(item.getQuantity());

            try {
                UUID productId = UUID.fromString(item.getProductId());
                UUID branchId = item.getBranchId().isEmpty() ? null : UUID.fromString(item.getBranchId());
                
                String resolvedBranchId = inventoryService.reserveStock(
                        productId, 
                        branchId, 
                        item.getQuantity(), 
                        request.getOrder().getOrderId(), 
                        request.getStrictBranch()
                );
                
                itemResult.setBranchId(resolvedBranchId);
                itemResult.setSuccess(true);
            } catch (IllegalStateException e) {
                success = false;
                itemResult.setSuccess(false);
                itemResult.setErrorCode("INSUFFICIENT_STOCK");
            } catch (Exception e) {
                success = false;
                itemResult.setSuccess(false);
                itemResult.setErrorCode(e.getMessage());
            }
            responseBuilder.addResults(itemResult.build());
        }

        responseBuilder.setSuccess(success);
        if (!success) {
            responseBuilder.setError(ErrorInfo.newBuilder()
                    .setCode("RESERVE_FAILED")
                    .setMessage("Failed to reserve some items")
                    .build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void commitStock(CommitRequest request, StreamObserver<CommitResponse> responseObserver) {
        log.info("Received commit request for order: {}", request.getOrder().getOrderId());
        try {
            inventoryService.commitStock(request.getOrder().getOrderId());
            responseObserver.onNext(CommitResponse.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            log.error("Error committing stock", e);
            responseObserver.onNext(CommitResponse.newBuilder()
                    .setSuccess(false)
                    .setError(ErrorInfo.newBuilder().setCode("COMMIT_FAILED").setMessage(e.getMessage()).build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void releaseStock(ReleaseRequest request, StreamObserver<ReleaseResponse> responseObserver) {
        log.info("Received release request for order: {}, reason: {}", request.getOrder().getOrderId(), request.getReason());
        try {
            inventoryService.releaseStock(request.getOrder().getOrderId());
            responseObserver.onNext(ReleaseResponse.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            log.error("Error releasing stock", e);
            responseObserver.onNext(ReleaseResponse.newBuilder()
                    .setSuccess(false)
                    .setError(ErrorInfo.newBuilder().setCode("RELEASE_FAILED").setMessage(e.getMessage()).build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void checkAvailability(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        CheckResponse.Builder responseBuilder = CheckResponse.newBuilder();
        for (InventoryItem item : request.getItemsList()) {
            UUID productId = UUID.fromString(item.getProductId());
            UUID branchId = item.getBranchId().isEmpty() ? null : UUID.fromString(item.getBranchId());
            int available = inventoryService.computeAvailable(productId, branchId);
            AvailabilityResult.Builder b = AvailabilityResult.newBuilder()
                    .setProductId(item.getProductId())
                    .setAvailable(available)
                    .setSufficient(available >= item.getQuantity());
            if (branchId != null) {
                b.setBranchId(branchId.toString());
            }
            responseBuilder.addResults(b.build());
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
