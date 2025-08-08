package com.zanchi.zanchi_backend.domain.member;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {
    @Id @GeneratedValue
    private Long id;

    private String name;

    private String loginId;

    private String password;

    // Spring Security와 JWT 인증 필터를 구현할 때 권한 정보가 필요하기 때문에 추가
    private String role;
}