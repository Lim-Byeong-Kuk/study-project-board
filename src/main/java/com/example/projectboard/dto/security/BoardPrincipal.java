package com.example.projectboard.dto.security;

import com.example.projectboard.dto.UserAccountDto;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record BoardPrincipal(
        String username,
        String password,

        Collection<? extends GrantedAuthority> authorities,
        String email,
        String nickname,
        String memo,
        Map<String, Object> oAuth2Attributes

) implements UserDetails, OAuth2User {

    public static BoardPrincipal of(String username, String password, String email, String nickname, String memo) {
        return of(username, password, email, nickname, memo, Map.of());
    }

    public static BoardPrincipal of(String username, String password, String email, String nickname, String memo, Map<String, Object> oAuth2Attributes) {
        // 지금은 인증만 하고 권한을 다루고 있지 않아서 임의로 세팅한다.
        Set<RoleType> roleTypes = Set.of(RoleType.USER);

        return new BoardPrincipal(
                username,
                password,
                roleTypes.stream()
                        .map(RoleType::getName)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toUnmodifiableSet()),
                email,
                nickname,
                memo,
                oAuth2Attributes
        );
    }

    public static BoardPrincipal from(UserAccountDto dto) {
        return BoardPrincipal.of(
                dto.userId(),
                dto.userPassword(),
                dto.email(),
                dto.nickname(),
                dto.memo()
        );
    }

    public UserAccountDto toDto() {
        return UserAccountDto.of(
                username,
                password,
                email,
                nickname,
                memo
        );
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    // 권한 설정 : 자기 글만 쓰고 지을 수 있도록
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    // 디테일한 설정은 하지않고 스프링 시큐리티 설정에 위임하기 때문에 true 로 반환
    // 관련된 룰을 정하고 설정한다면 그때 다시 보면 될 것
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public Map<String, Object> getAttributes() { return oAuth2Attributes; }
    @Override
    public String getName() { return username; }

    public enum RoleType {
        USER("ROLE_USER"); // ROLE_ 이렇게 시작하는것은 스프링시큐리티의 규칙이다.

        @Getter
        private final String name;

        RoleType(String name) {
            this.name = name;
        }
    }

}
