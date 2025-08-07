package com.zanchi.zanchi_backend.domain.member;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시용
@AllArgsConstructor
@Builder

public class Member {
    @Id @GeneratedValue
    private Long id;

    private String name;

    private String email;

    private String password;
}