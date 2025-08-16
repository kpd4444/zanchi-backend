package com.zanchi.zanchi_backend.domain.clip;

import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clip_comment_mention")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClipCommentMention {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comment_id", nullable = false)
    private ClipComment comment;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "mentioned_member_id", nullable = false)
    private Member mentioned;

    @Column(name = "handle_snapshot", nullable = false, length = 64)
    private String handleSnapshot; // 당시 @handle 기록

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
