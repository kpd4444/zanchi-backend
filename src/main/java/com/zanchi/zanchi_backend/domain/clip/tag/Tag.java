package com.zanchi.zanchi_backend.domain.clip.tag;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_normalized_name", columnNames = "normalized_name"),
        indexes = { @Index(name="idx_tag_normalized_name", columnList = "normalized_name") })
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name", nullable = false, length = 50)
    private String name;

    @Column(name="normalized_name", nullable = false, length = 50)
    private String normalizedName;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
