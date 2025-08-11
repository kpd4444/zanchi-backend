package com.zanchi.zanchi_backend.domain.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 로그인 아이디로 조회
    Optional<Member> findByLoginId(String loginId);

    // 회원가입 중복 체크용
    boolean existsByLoginId(String loginId);

    // 선호도 조사 1회 보장 등 동시성 제어가 필요할 때 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);
}
