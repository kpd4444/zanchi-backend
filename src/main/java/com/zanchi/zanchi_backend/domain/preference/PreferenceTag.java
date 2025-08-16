package com.zanchi.zanchi_backend.domain.preference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "preference_tag",
        indexes = { @Index(name="idx_preference_tag_code", columnList="code", unique = true) })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PreferenceTag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String code; // 내부 코드 (예: K_POP)

    @Column(length = 50, nullable = false)
    private String name; // 노출명 (예: "K-POP")
}
