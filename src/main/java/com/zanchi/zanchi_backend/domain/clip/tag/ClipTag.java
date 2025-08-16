package com.zanchi.zanchi_backend.domain.clip.tag;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "clip_tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_clip_tag", columnNames = {"clip_id","tag_id"}),
        indexes = {
                @Index(name="idx_clip_tag_tag", columnList = "tag_id"),
                @Index(name="idx_clip_tag_clip", columnList = "clip_id")
        })
public class ClipTag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clip_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_cliptag_clip"))
    private Clip clip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_cliptag_tag"))
    private Tag tag;
}
