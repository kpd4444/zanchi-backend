package com.zanchi.zanchi_backend.domain.clip;

import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "clip_comments")
public class ClipComment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clip_id", nullable = false)
    private Clip clip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Member author;

    @Column(nullable = false, length = 500)
    private String content;

    private LocalDateTime createdAt;

    // ✅ 부모 댓글 (null이면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ClipComment parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClipComment> replies = new ArrayList<>();


    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addReply(ClipComment reply) {
        reply.setParent(this);
        this.replies.add(reply);
    }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof ClipComment c && id!=null && id.equals(c.id));}
    @Override public int hashCode(){ return 31; }
}