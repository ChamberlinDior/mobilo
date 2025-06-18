package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entité « ride » (course).
 *
 * <p>Ajouts 2025-06-20 :</p>
 * <ul>
 *   <li><strong>riderUid</strong> – identifiant fonctionnel (UUID, etc.) utilisé
 *       par l’API Wallet ; permet d’appeler <code>UserRepository.findByUid()</code>.</li>
 * </ul>
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
                @Index(name = "idx_ride_rider_id",   columnList = "rider_id"),
                @Index(name = "idx_ride_rider_uid",  columnList = "rider_uid"),
                @Index(name = "idx_ride_driver_id",  columnList = "driver_id"),
                @Index(name = "idx_ride_status",     columnList = "status")
        }
)
public class Ride {

    /* ───────────── Identité ───────────── */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ───────────── Pool / groupage ────── */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_group_id")
    private PoolGroup poolGroup;

    /* ───────────── Participants ───────── */

    /** Identifiant numérique interne (clé étrangère classique).
     *  Devient optionnel : le backend peut le déduire du JWT. */
    @Column(name = "rider_id", nullable = true, updatable = false)
    private Long riderId;

    /**
     * Identifiant fonctionnel (UUID, slug…)
     * utilisé par les APIs de lecture / wallet.
     * Peut être null si non fourni.
     */
    @Column(name = "rider_uid", length = 64, nullable = true, updatable = false)
    private String riderUid;

    /** Peut rester {@code null} tant qu’aucun chauffeur n’est attribué. */
    @Column(name = "driver_id")
    private Long driverId;

    /* ★ Moyen de paiement sélectionné (carte, CASH…) */
    @Column(name = "pay_method_id")
    private Long paymentMethodId;

    /* ───────────── Localisation ───────── */

    @Column(name = "pickup_lat",  nullable = false)
    private Double pickupLat;

    @Column(name = "pickup_lng",  nullable = false)
    private Double pickupLng;

    @Column(name = "dropoff_lat", nullable = false)
    private Double dropoffLat;

    @Column(name = "dropoff_lng", nullable = false)
    private Double dropoffLng;

    /* Libellés complets d’adresse (facultatifs) */
    @Column(name = "pickup_address",  length = 255)
    private String pickupAddress;

    @Column(name = "dropoff_address", length = 255)
    private String dropoffAddress;

    /* ───────────── Produit & options ─── */

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 16)
    private ProductType productType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ride_options",
            joinColumns = @JoinColumn(name = "ride_id"))
    @Column(name = "option_name", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private List<RideOption> options;

    /* ───────────── Statut & timings ──── */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RideStatus status;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    /* ───────────── Tarification ──────── */

    @Column(name = "total_fare", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalFare;

    @Column(name = "currency", length = 8, nullable = false)
    private String currency;

    /* ───────────── Sécurité & méta ───── */

    @Column(name = "safety_pin", length = 4, updatable = false)
    private String safetyPin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /* ───────────── Hooks ORM ─────────── */

    @PrePersist
    private void prePersist() {
        createdAt = OffsetDateTime.now();

        if (safetyPin == null) {
            int n = ThreadLocalRandom.current().nextInt(0, 10_000);
            safetyPin = String.format("%04d", n);
        }
        if (status == null) {
            status = RideStatus.REQUESTED;
        }
    }
}
