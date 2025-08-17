package com.zanchi.zanchi_backend.domain.clip;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "clip_mention",
        uniqueConstraints = @UniqueConstraint(columnNames = {"clip_id", "mentioned_member_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClipMention {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clip_id", nullable = false)
    private Long clipId;

    @Column(name = "mentioned_member_id", nullable = false)
    private Long mentionedMemberId;
}