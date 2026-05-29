package com.thluxury.order.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private String eventData;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public OrderEvent() {}

    public OrderEvent(UUID orderId, int sequenceNo, String eventType, String eventData) {
        this.orderId = orderId;
        this.sequenceNo = sequenceNo;
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public int getSequenceNo() { return sequenceNo; }
    public String getEventType() { return eventType; }
    public String getEventData() { return eventData; }
    public Instant getOccurredAt() { return occurredAt; }
}
