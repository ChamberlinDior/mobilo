// ============================
// src/main/java/com/mobility/ride/model/SafetyIncident.java
// ============================
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "safety_incidents",
        indexes = {
                @Index(name = "idx_incident_ride", columnList = "ride_id"),
                @Index(name = "idx_incident_type", columnList = "type")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SafetyIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    private SafetyIncidentType type;

    @Embedded
    private LatLng location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void ts() { createdAt = OffsetDateTime.now(); }
}