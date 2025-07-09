// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/model/Ride.java
//  v2025-09-08 – @Version NOT-NULL + baseline 0  + minor clean-ups
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entité « Ride » – cycle de vie complet d’une course ou livraison
 * (REQUESTED → … → COMPLETED / CANCELLED / NO_SHOW).
 *
 * Nouveautés 2025-09-08 :
 * • Colonne `version` déclarée NOT NULL DEFAULT 0 pour fiabilité du verrou
 *   pessimiste et migration sans NULL version.
 * • Pré-persist : si la version est encore nulle ⇒ 0L (sécurité).
 * • Aucune ligne supprimée par rapport à la v2025-09-02.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "rides",
        indexes = {
                @Index(name = "idx_ride_rider_id",  columnList = "rider_id"),
                @Index(name = "idx_ride_rider_uid", columnList = "rider_uid"),
                @Index(name = "idx_ride_driver_id", columnList = "driver_id"),
                @Index(name = "idx_ride_status",    columnList = "status")
        }
)
public class Ride {

    /* ───────────── Identité ───────────── */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ─────────── Groupage / Pool ──────── */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_group_id")
    private PoolGroup poolGroup;

    /* ─────────── Participants ─────────── */

    @Column(name = "rider_id", updatable = false)
    private Long riderId;

    @Column(name = "rider_uid", length = 64, updatable = false)
    private String riderUid;

    @Column(name = "driver_id")
    private Long driverId;

    /** Moyen de paiement sélectionné (null ⇒ CASH) */
    @Column(name = "pay_method_id")
    private Long paymentMethodId;

    /* ─────────── Localisation ─────────── */

    @Column(name = "pickup_lat",  nullable = false) private Double pickupLat;
    @Column(name = "pickup_lng",  nullable = false) private Double pickupLng;
    @Column(name = "dropoff_lat", nullable = false) private Double dropoffLat;
    @Column(name = "dropoff_lng", nullable = false) private Double dropoffLng;

    @Column(name = "pickup_address",  length = 255)
    private String pickupAddress;

    @Column(name = "dropoff_address", length = 255)
    private String dropoffAddress;

    /* ───────── Produit & options ─────── */

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 16)
    private ProductType productType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "ride_options",
            joinColumns = @JoinColumn(name = "ride_id")
    )
    @Column(name = "option_name", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private List<RideOption> options;

    /* ───────── Statut & timings ───────── */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RideStatus status;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    /* Horodatages temps réel chauffeur */
    @Column(name = "accepted_at")     private OffsetDateTime acceptedAt;
    @Column(name = "en_route_at")     private OffsetDateTime enRouteAt;
    @Column(name = "arrived_at")      private OffsetDateTime arrivedAt;
    @Column(name = "pickup_real_at")  private OffsetDateTime pickupRealAt;
    @Column(name = "dropoff_real_at") private OffsetDateTime dropoffRealAt;
    @Column(name = "cancelled_at")    private OffsetDateTime cancelledAt;
    @Column(name = "cancel_reason", length = 32)
    private String cancelReason;

    /* Attente payante */
    @Column(name = "waiting_sec")
    private Integer waitingSec;                 // secondes

    @Column(name = "wait_fee", precision = 10, scale = 2)
    private BigDecimal waitFee;

    /* ─────────── Tarification ─────────── */

    @Column(name = "total_fare", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalFare;

    @Column(name = "currency", length = 8, nullable = false)
    private String currency;

    /* Distance / durée réelles */
    @Column(name = "distance_km_real")
    private Double distanceKmReal;

    @Column(name = "duration_sec_real")
    private Long durationSecReal;

    /* ─────────── Livraison / poids ────── */

    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_zone", length = 32)
    private DeliveryZone deliveryZone;

    /* ─────────── Sécurité / méta ─────── */

    @Column(name = "safety_pin", length = 4, updatable = false)
    private String safetyPin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /* Optimistic-locking – NOT NULL to avoid lock exceptions */
    @Version
    @Column(
            name = "version",
            nullable = false,
            columnDefinition = "BIGINT NOT NULL DEFAULT 0"
    )
    private Long version;

    /* ─────────── Hooks ORM ───────────── */

    @PrePersist
    private void prePersist() {
        createdAt = OffsetDateTime.now();

        if (safetyPin == null) {
            safetyPin = String.format("%04d",
                    ThreadLocalRandom.current().nextInt(0, 10_000));
        }
        if (status == null)     status     = RideStatus.REQUESTED;
        if (waitingSec == null) waitingSec = 0;
        if (waitFee == null)    waitFee    = BigDecimal.ZERO;
        if (version == null)    version    = 0L;          // ←  garantit NOT NULL
    }
}
