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
@Table(name = "clips")
public class Clip {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id")
    private Member uploader; // 작성자

    @Column(length = 300)
    private String caption;

    @Column(nullable = false)
    private String videoUrl;

    @Builder.Default
    @OneToMany(mappedBy = "clip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClipComment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "clip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClipLike> likes = new ArrayList<>();

    private long viewCount;

    private long likeCount;
    private long commentCount;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addComment(ClipComment c) {
        c.setClip(this);
        this.comments.add(c);
    }
    public void addLike(ClipLike l) {
        l.setClip(this);
        this.likes.add(l);
    }

    // equals/hashCode 는 식별자만
    @Override public boolean equals(Object o){ return (this==o) || (o instanceof Clip c && id!=null && id.equals(c.id));}
    @Override public int hashCode(){ return 31; }
}