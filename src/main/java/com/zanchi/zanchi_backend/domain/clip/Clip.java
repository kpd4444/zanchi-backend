package com.zanchi.zanchi_backend.domain.clip;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zanchi.zanchi_backend.domain.comment.Comment;
import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class Clip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private Member uploader;

    @Column(length = 1000)
    private String caption;

    @Column(nullable = false)
    private String videoUrl;      // /uploads/clips/xxx.mp4

    private String thumbnailUrl;  // 선택: 썸네일 생성 시

    @Builder.Default
    private long viewCount = 0;

    @Builder.Default
    private long likeCount = 0;

    @Builder.Default
    private long commentCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "clip", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ClipComment> comments = new ArrayList<>();
}