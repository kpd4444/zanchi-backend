package com.zanchi.zanchi_backend.domain.notification.service;

import java.util.Map;
import java.util.Set;

public interface MemberLookupPort {
    record MemberBrief(String nickname, String avatarUrl) {}
    MemberBrief EMPTY = new MemberBrief("", "");

    String nickname(Long memberId);
    Map<Long, MemberBrief> bulk(Set<Long> ids);
}
