// ============================
// src/main/java/com/mobility/ride/model/Tip.java
// ============================
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tips", indexes = @Index(name = "idx_tip_ride", columnList = "ride_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Tip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void ts() { createdAt = OffsetDateTime.now(); }
}
