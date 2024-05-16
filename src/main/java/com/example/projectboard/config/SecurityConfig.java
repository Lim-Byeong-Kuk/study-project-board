package com.example.projectboard.config;

import com.example.projectboard.dto.security.BoardPrincipal;
import com.example.projectboard.dto.security.KakaoOAuth2Response;
import com.example.projectboard.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

@Configuration
public class SecurityConfig {

    // 시큐리티를 태워서 스프링  시큐리티의 관리하에 두고 인증과 권한 체크를 하는 부분
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService
    ) throws Exception {
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
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .oauth2Login(oAuth -> oAuth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                )
                .build();
    }

    // 인증 정보, 사용자 정보를 가져오는 부분
    @Bean
    public UserDetailsService userDetailsService(UserAccountService userAccountService) {
        return username -> userAccountService
                .searchUser(username)
                .map(BoardPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다 - username: " + username));
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService(
            UserAccountService userAccountService,
            PasswordEncoder passwordEncoder
    ) {
        final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService(); // 기본 구현체

        return userRequest -> {
            OAuth2User oAuth2User = delegate.loadUser(userRequest);
            // 기본 동작을 끝낸 뒤 우리 DB의 그 OAuth2 카카오 인증서버로부터 가져온 인증정보를 저장하고싶다.

            KakaoOAuth2Response kakaoResponse = KakaoOAuth2Response.from(oAuth2User.getAttributes());

            // username 은 카카오 OAuth REST API 응답으로부터 가져올 수 었다. 우리가 새로 만들어 줘야 한다.
            // 그리고 고유값이어야 한다. 여기서 username 은 UserAccount 에서 userId 를 말함
            // RegistrationId 는 yaml 에서 설정했던 registration 을 말한다. 즉 "kakao" 를 말한다.
            // kakaoResponse.id() 는 카카오 REST API 응답으로 받은 유저의 고유 id 를 의미
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String providerId = String.valueOf(kakaoResponse.id());
            String username = registrationId + "_" + providerId;        // 고유값을 만들어냄
            // 사실 패스워드는 필요 없어야 하는데 회원 테이블을 설계할 때 패스워드를 기본 필수값으로 지정했기 때문에 넣어주는것
            String dummyPassword = passwordEncoder.encode("{bcrypt}" + UUID.randomUUID()); // bcrypt 는 DB에 인코딩해서 넣어준다.

            // DB에 유저가 있다면 그걸로 OK, DB에 유저가 없다면 저장을 하도록 하겠다.
            return userAccountService.searchUser(username)
                    .map(BoardPrincipal::from)
                    .orElseGet(() ->
                            BoardPrincipal.from(
                                    userAccountService.saveUser(
                                            username,
                                            dummyPassword,
                                            kakaoResponse.email(),
                                            kakaoResponse.nickname(),
                                            null
                                    )
                            )
                    );
        };

    }

    // 스프링 시큐리티의 인증기능을 이용할 때에는 반드시 password encoder 도 등록을 해줘야 한다.
    // password encoder 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
