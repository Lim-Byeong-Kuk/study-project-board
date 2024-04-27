package com.example.projectboard.config;

import com.example.projectboard.dto.UserAccountDto;
import com.example.projectboard.dto.security.BoardPrincipal;
import com.example.projectboard.repository.UserAccountRepository;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // 시큐리티를 태워서 스프링  시큐리티의 관리하에 두고 인증과 권한 체크를 하는 부분
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .mvcMatchers(
                                HttpMethod.GET,
                                "/",
                                "/articles",
                                "/articles/search-hashtag"
                        ).permitAll() // GET이 아닌 포스트 (삭제, 추가, 수정) 에 대해서는 권한 체킹을 하겠다는 의도
                        .anyRequest().authenticated() // 나머지 anyRequest는 인증되어야만 함
                )
                .formLogin().and()  // 로그인페이지를 만들도록
                .logout()
                        .logoutSuccessUrl("/")
                        .and()
                .build();
    }

    // 인증 정보, 사용자 정보를 가져오는 부분
    @Bean
    public UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        return username -> userAccountRepository
                .findById(username)
                .map(UserAccountDto::from)
                .map(BoardPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다 - username: " + username));
    }

    // 스프링 시큐리티의 인증기능을 이용할 때에는 반드시 password encoder 도 등록을 해줘야 한다.
    // password encoder 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
