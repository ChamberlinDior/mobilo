// ============================
// src/main/java/com/mobility/ride/model/SurgeMultiplier.java
// ============================
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entité de table de multiplicateurs de tarification dynamique (surge).
 */
@Entity
@Table(name = "surge_multipliers",
        indexes = {
                @Index(name = "idx_surge_city_product", columnList = "city_id, product_type"),
                @Index(name = "idx_surge_window", columnList = "window_start, window_end")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_surge_city_product_window",
                columnNames = {"city_id", "product_type", "window_start"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SurgeMultiplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 16)
    private ProductType productType;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal factor;

    @Column(name = "window_start", nullable = false)
    private OffsetDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private OffsetDateTime windowEnd;
}
