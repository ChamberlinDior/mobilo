package com.mobility.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** libellé : "Home", "Work", etc. */
    @NotBlank
    @Column(nullable = false, length = 60)
    private String label;

    /** texte complet de l’adresse */
    @NotBlank
    @Column(nullable = false, length = 255)
    private String address;

    private Double latitude;
    private Double longitude;
}
