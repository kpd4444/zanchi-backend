package com.zanchi.zanchi_backend.domain.notification.service;

import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class MemberLookupAdapter implements MemberLookupPort {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public String nickname(Long memberId) {
        // 닉네임 = Member.name
        return memberRepository.findById(memberId)
                .map(Member::getName)
                .orElse("알 수 없음");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, MemberBrief> bulk(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        Iterable<Member> found = memberRepository.findAllById(ids);
        return StreamSupport.stream(found.spliterator(), false)
                .collect(Collectors.toMap(
                        Member::getId,
                        m -> new MemberBrief(
                                m.getName(),
                                Optional.ofNullable(m.getAvatarUrl()).orElse("")
                        )
                ));
    }
}
