// ============================
// src/main/java/com/mobility/ride/model/PaymentSplit.java
// ============================
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "payment_splits", indexes = @Index(name = "idx_split_ride", columnList = "ride_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @Column(name = "share_pct", nullable = false)
    private Integer sharePct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void ts() { createdAt = OffsetDateTime.now(); }
}
