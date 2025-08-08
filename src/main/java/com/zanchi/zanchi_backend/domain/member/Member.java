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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MariaDB/H2 호환 안전
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 30, unique = true) // 로그인 중복 방지 & 빠른 조회
    private String loginId;

    @Column(nullable = false, length = 60)// BCrypt 길이 고려
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_USER"; // MemberPrincipal의 권한 매핑에 필요
}