package com.zanchi.zanchi_backend.domain.clip;

import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="clip_save",
        uniqueConstraints = @UniqueConstraint(name="uk_member_clip", columnNames={"member_id","clip_id"}))
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClipSave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="member_id", nullable=false)
    private Member member;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="clip_id", nullable=false)
    private Clip clip;

    private LocalDateTime createdAt;

    @PrePersist void pre() { if (createdAt==null) createdAt = LocalDateTime.now(); }
}