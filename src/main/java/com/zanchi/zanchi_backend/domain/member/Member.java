package com.zanchi.zanchi_backend.domain.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Entity
@Table(
        name = "member",
        indexes = @Index(name = "idx_member_login_id", columnList = "loginId", unique = true)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MariaDB/H2 호환 안전
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 30, unique = true) // 로그인 중복 방지 & 빠른 조회
    private String loginId;

    @Column(nullable = false, length = 60) // BCrypt 길이 고려
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_USER"; // MemberPrincipal의 권한 매핑에 필요

    @Column(length = 255)
    private String avatarUrl;

    @Column(name = "preference_survey_completed", nullable = false)
    private boolean preferenceSurveyCompleted;

    @Column(nullable = false)
    @Builder.Default
    private Integer point = 0;

    /* -------------------- 신규 추가 필드 -------------------- */

    @Column(name = "first_login_at")
    private LocalDateTime firstLoginAt; // 최초 로그인 시각 (null이면 아직 첫 로그인 전)

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;  // 마지막 로그인 시각

    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private Integer loginCount = 0;     // 총 누적 로그인 횟수

    /* -------------------- 도메인 메서드 -------------------- */

    public void addPoint(int amount) {
        this.point += amount;
    }

    public void usePoint(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        if (this.point < amount) throw new IllegalStateException("insufficient points");
        this.point -= amount;
    }

    public void markPreferenceSurveyCompleted() {
        this.preferenceSurveyCompleted = true;
    }
}
