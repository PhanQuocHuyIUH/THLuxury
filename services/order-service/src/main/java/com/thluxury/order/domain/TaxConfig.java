package com.thluxury.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tax_config")
public class TaxConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @PrePersist
    void onCreate() { if (effectiveFrom == null) effectiveFrom = Instant.now(); }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal v) { this.percent = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
}
