package com.zanchi.zanchi_backend.config.security;

import com.zanchi.zanchi_backend.domain.member.Member;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class MemberPrincipal implements UserDetails {

    private final Member member;

    public MemberPrincipal(Member member) {
        this.member = member;
    }


    @Override
    public String getPassword() {
        return member.getPassword();
    }

    public Long getId() {
        return member.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String r = member.getRole();
        if (r == null || r.isBlank()) {
            // 기본 권한 부여 (원하면 USER 대신 원하는 기본값으로)
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        String roleName = r.startsWith("ROLE_") ? r : "ROLE_" + r;
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getUsername() {
        return member.getLoginId();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}