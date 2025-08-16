package com.zanchi.zanchi_backend.domain.preference;

import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_preference")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberPreference {

    @EmbeddedId
    private MemberPreferenceId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @MapsId("preferenceTagId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_tag_id")
    private PreferenceTag preferenceTag;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
