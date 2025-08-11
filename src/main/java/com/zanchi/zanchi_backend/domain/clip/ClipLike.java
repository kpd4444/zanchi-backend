package com.zanchi.zanchi_backend.domain.clip;

import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "clip_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_clip_like", columnNames = {"clip_id","member_id"}))
public class ClipLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clip_id")
    private Clip clip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof ClipLike c && id!=null && id.equals(c.id));}
    @Override public int hashCode(){ return 31; }
}