package com.zanchi.zanchi_backend.domain.login;

import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class LoginService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public String login(String loginId, String password) {
        return memberRepository.findByLoginId(loginId)
                .filter(m -> m.getPassword().equals(password))
                .map(member -> jwtTokenProvider.createToken(member.getLoginId()))
                .orElse(null);
    }
}