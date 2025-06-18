// ─────────────────────────────────────────────────────────────────────────────
// PACKAGE : com.mobility.ride.model
// ----------------------------------------------------------------------------
package com.mobility.ride.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_messages",
        indexes = @Index(name = "idx_chat_ride", columnList = "ride_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 2000)
    private String text;

    private OffsetDateTime createdAt;

    @PrePersist void ts() { createdAt = OffsetDateTime.now(); }
}
