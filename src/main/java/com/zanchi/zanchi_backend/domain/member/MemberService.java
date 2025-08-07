package com.zanchi.zanchi_backend.domain.member;

import com.zanchi.zanchi_backend.web.member.form.MemberForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public void signup(MemberForm form) {
        if (memberRepository.findByLoginId(form.getLoginId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 로그인 ID입니다.");
        }

        Member member = Member.builder()
                .name(form.getName())
                .loginId(form.getLoginId())
                .password(form.getPassword()) //지인아 여기 암호화 해야해 나중에 시간나면 하자
                .build();

        memberRepository.save(member);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public void deleteById(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
        memberRepository.deleteById(id);
    }
}
