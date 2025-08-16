package com.zanchi.zanchi_backend.domain.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    // 이름 또는 로그인ID 검색 (대소문자 무시)
    Page<Member> findByNameContainingIgnoreCaseOrLoginIdContainingIgnoreCase(
            String name, String loginId, Pageable pageable);

    // 로그인ID로 ID만 조회
    @Query("select m.id from Member m where m.loginId = :loginId")
    Optional<Long> findIdByLoginId(@Param("loginId") String loginId);

    boolean existsByNameIgnoreCase(String name);

    @Query("select m from Member m where m.loginId in :handles")
    List<Member> findAllByLoginIdIn(@Param("handles") Collection<String> handles);
}
