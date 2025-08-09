package com.zanchi.zanchi_backend.domain.preference.repository;

import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.preference.MemberPreference;
import com.zanchi.zanchi_backend.domain.preference.MemberPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberPreferenceRepository extends JpaRepository<MemberPreference, MemberPreferenceId> {
    List<MemberPreference> findByMember(Member member);
    long countByMember(Member member);
}
