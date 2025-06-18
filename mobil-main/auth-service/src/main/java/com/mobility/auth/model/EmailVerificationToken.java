// com.mobility.auth.model.EmailVerificationToken.java
package com.mobility.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "email_verification_tokens",
        indexes = @Index(name = "idx_ev_token", columnList = "token"),
        uniqueConstraints = @UniqueConstraint(columnNames = "token")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @PrePersist
    public void onPersist() {
        this.expiresAt = OffsetDateTime.now().plusHours(24);
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }
}
