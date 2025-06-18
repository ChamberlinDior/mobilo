package com.mobility.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_documents",
        indexes = @Index(name = "idx_user_documents_type", columnList = "doc_type")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)  // FK user_id
    @JoinColumn(nullable = false)
    private User user;

    @Column(name = "doc_type", length = 32, nullable = false)
    private String documentType;     // ex: "ID_CARD", "LICENSE", …

    @Column(length = 255, nullable = false)
    private String filename;         // nom original du fichier

    @Column(length = 32, nullable = false)
    private String mimeType;         // ex: "image/jpeg"

    /**
     * Clé renvoyée par le StorageService (S3, GCS, …)
     */
    @Column(length = 512, nullable = false)
    private String dataKey;

    private OffsetDateTime uploadedAt;

    @PrePersist
    void onUpload() {
        this.uploadedAt = OffsetDateTime.now();
    }
}
