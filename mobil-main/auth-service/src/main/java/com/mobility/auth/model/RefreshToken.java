package com.mobility.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = @Index(name = "idx_refresh_token_value", columnList = "token"),
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_value", columnNames = "token")
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    /** ⬇️ valeur appliquée même lorsqu’on utilise RefreshToken.builder() … */
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private Boolean revoked = Boolean.FALSE;

    @PrePersist
    void ts() { createdAt = OffsetDateTime.now(); }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }
}
