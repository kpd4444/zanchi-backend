package com.zanchi.zanchi_backend.domain.report;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "clip_report",
        uniqueConstraints = {
                // 같은 사용자가 같은 클립을 중복 신고(스팸)하지 못하게 optional
                @UniqueConstraint(name="uk_clip_report_unique", columnNames = {"clip_id","reporter_id"})
        }
)
@Getter
@Setter
public class ClipReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="clip_id", nullable=false)
    private Clip clip;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="reporter_id", nullable=false)
    private Member reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private ReportReason reason;

    @Column(length=1000)
    private String detail;           // 상세 설명(선택)

    @Column(nullable=false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}