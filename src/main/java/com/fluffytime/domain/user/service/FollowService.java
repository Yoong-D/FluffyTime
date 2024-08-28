package com.fluffytime.domain.user.service;

import com.fluffytime.domain.user.dto.request.FollowRequest;
import com.fluffytime.global.auth.jwt.util.JwtTokenizer;
import com.fluffytime.global.common.exception.global.UserNotFound;
import com.fluffytime.domain.user.entity.Follow;
import com.fluffytime.domain.user.entity.User;
import com.fluffytime.domain.user.repository.FollowRepository;
import com.fluffytime.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final JwtTokenizer jwtTokenizer;

    // 팔로우 여부 단 건 확인 메서드
    public boolean isFollowing(Long followingId, Long followedId) {
        return followRepository.findByFollowingUserUserIdAndFollowedUserUserId(followingId,
            followedId).isPresent();
    }

    //팔로우 등록
    @Transactional
    public void follow(FollowRequest followRequest) {

        // User를 db에서 조회
        User followingUser = userRepository.findById(followRequest.getFollowingId())
            .orElseThrow(UserNotFound::new);
        User followedUser = userRepository.findByNickname(
                followRequest.getFollowedUserNickname())
            .orElseThrow(UserNotFound::new);

        // Follow 엔티티로 변환
        Follow follow = new Follow();
        follow.setFollowingUser(followingUser);
        follow.setFollowedUser(followedUser);

        followRepository.save(follow);
    }

    //언팔로우 (팔로우 취소)
    @Transactional
    public void unfollow(FollowRequest followRequest) {

        // 팔로우 관계를 찾기 위해 followingUserId와 followedUserId로 Follow 엔티티를 조회
        Follow follow = followRepository.findByFollowingUserUserIdAndFollowedUserUserId(
                followRequest.getFollowingId(),
                findUserByNickname(followRequest.getFollowedUserNickname()).getUserId())
            .orElseThrow(UserNotFound::new);//TODO FollowNotFound로 바꾸기

        // 팔로우 관계 삭제
        followRepository.delete(follow);
    }

    // accessToken 토큰으로 사용자 찾기
    @Transactional
    public User findByAccessToken(HttpServletRequest httpServletRequest) {
        log.info("findByAccessToken 실행");
        String accessToken = null;

        // accessToken 값 추출
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                }
            }
        }
        // accessToken 으로 UserId 추출
        Long userId = null;
        userId = jwtTokenizer.getUserIdFromToken(accessToken);
        User user = findUserById(userId).orElseThrow(UserNotFound::new);
        return user;
    }

    //사용자 조회
    public Optional<User> findUserById(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user;
    }


    @Transactional
    public User findUserByNickname(String nickname) {
        Optional<User> user = userRepository.findByNickname(nickname);

        return user.orElseThrow(UserNotFound::new);
    }
}