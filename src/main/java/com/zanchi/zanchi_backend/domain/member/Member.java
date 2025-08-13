package com.zanchi.zanchi_backend.domain.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;

    @Column(nullable = false, length = 30, unique = true)
    private String loginId;

    @Column(nullable = false, length = 60)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_USER";

    @Column(name = "preference_survey_completed", nullable = false)
    private boolean preferenceSurveyCompleted;

    @Column(nullable = false)
    @Builder.Default
    private Integer point = 0;

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
