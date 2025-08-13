package com.zanchi.zanchi_backend.domain.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface MemberRepository extends JpaRepository<Member,Long> {
    Optional<Member> findByLoginId(String loginId);
    Page<Member> findByNameContainingIgnoreCaseOrLoginIdContainingIgnoreCase(
            String name, String loginId, Pageable pageable);

    @Query("select m.id from Member m where m.loginId = :loginId")
    Optional<Long> findIdByLoginId(@Param("loginId") String loginId);
}
