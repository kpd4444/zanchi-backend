package com.zanchi.zanchi_backend.domain.clip;

import com.zanchi.zanchi_backend.domain.clip.tag.ClipTag;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.show.Show;
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
    private Member uploader;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "show_id")       // DB 컬럼명
    private Show show;

    @Column(length = 300)
    private String caption;

    @Column(nullable = false)
    private String videoUrl;

    /** 댓글 */
    @Builder.Default
    @OneToMany(
            mappedBy = "clip",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<ClipComment> comments = new ArrayList<>();

    /** 좋아요 */
    @Builder.Default
    @OneToMany(
            mappedBy = "clip",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<ClipLike> likes = new ArrayList<>();

    /** 저장(북마크) — clip_save FK 때문에 꼭 필요 */
    @Builder.Default
    @OneToMany(
            mappedBy = "clip",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<ClipSave> saves = new ArrayList<>();

    /** 태그 조인(있다면) */
    @Builder.Default
    @OneToMany(
            mappedBy = "clip",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private List<ClipTag> tags = new ArrayList<>();

    private long viewCount;
    private long likeCount;
    private long commentCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = this.createdAt; }
    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    /* ===== 편의 메서드 (수정/추가/삭제가 컬렉션에 반영되도록) ===== */
    public void addComment(ClipComment c){ c.setClip(this); comments.add(c); }
    public void removeComment(ClipComment c){ comments.remove(c); c.setClip(null); }

    public void addLike(ClipLike l){ l.setClip(this); likes.add(l); }
    public void removeLike(ClipLike l){ likes.remove(l); l.setClip(null); }

    public void addSave(ClipSave s){ s.setClip(this); saves.add(s); }
    public void removeSave(ClipSave s){ saves.remove(s); s.setClip(null); }

    public void addTag(ClipTag t){ t.setClip(this); tags.add(t); }
    public void removeTag(ClipTag t){ tags.remove(t); t.setClip(null); }

    @Override public boolean equals(Object o){ return (this==o) || (o instanceof Clip c && id!=null && id.equals(c.id));}
    @Override public int hashCode(){ return 31; }
}