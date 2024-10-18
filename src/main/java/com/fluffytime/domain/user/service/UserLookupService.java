package com.fluffytime.domain.user.service;

import com.fluffytime.domain.user.entity.User;
import com.fluffytime.domain.user.repository.UserRepository;
import com.fluffytime.global.auth.jwt.exception.TokenNotFound;
import com.fluffytime.global.auth.jwt.util.JwtTokenizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserLookupService {
    private final UserRepository userRepository;
    private final JwtTokenizer jwtTokenizer;

    // 사용자 조회(userId로 조회) 메서드
    @Transactional
    public User findUserById(Long userId) {
        log.info("findUserById 실행");
        return userRepository.findById(userId).orElse(null);
    }

    // 사용자 조회(nickname으로 조회) 메서드
    @Transactional
    public User findUserByNickname(String nickname) {
        log.info("findUserByNickname 실행");
        return userRepository.findByNickname(nickname).orElse(null);
    }

    // accessToken 토큰으로 사용자 찾기 메서드
    @Transactional
    public User findByAccessToken(HttpServletRequest httpServletRequest) {
        log.info("findByAccessToken 실행");
        String accessToken = jwtTokenizer.getTokenFromCookie(httpServletRequest, "accessToken");
        if (accessToken == null) {
            throw new TokenNotFound();
        }
        // accessToken값으로 UserId 추출
        Long userId = Long.valueOf(
            ((Integer) jwtTokenizer.parseAccessToken(accessToken).get("userId")));
        // id(pk)에 해당되는 사용자 추출
        return findUserById(userId);
    }
}
