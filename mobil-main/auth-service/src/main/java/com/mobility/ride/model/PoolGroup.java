// ============================
// src/main/java/com/mobility/ride/model/PoolGroup.java
// ============================
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Groupe de trajets partagés (Pool).
 */
@Entity
@Table(name = "pool_groups",
        indexes = @Index(name = "idx_pool_status", columnList = "status"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PoolGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ---------------------- Points de trajet ---------------------- */

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "pickup_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "pickup_lng"))
    })
    private LatLng pickupLatLng;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "drop_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "drop_lng"))
    })
    private LatLng dropLatLng;

    /* ---------------------- Statut & relations ---------------------- */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PoolGroupStatus status;

    /** Courses rattachées à ce groupe (One Pool → Many Rides). */
    @OneToMany(mappedBy = "poolGroup", fetch = FetchType.LAZY)
    private Set<Ride> rides = new HashSet<>();

    /* ---------------------- Métadonnées ---------------------- */

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) status = PoolGroupStatus.FORMING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
