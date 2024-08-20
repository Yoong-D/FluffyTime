package com.fluffytime.userpage.service;

import com.fluffytime.auth.jwt.util.JwtTokenizer;
import com.fluffytime.domain.Profile;
import com.fluffytime.domain.ProfileImages;
import com.fluffytime.domain.TempStatus;
import com.fluffytime.domain.User;
import com.fluffytime.mypage.response.PostDto;
import com.fluffytime.mypage.service.MyPageService;
import com.fluffytime.repository.UserRepository;
import com.fluffytime.userpage.exception.UserPageNotFound;
import com.fluffytime.userpage.response.UserPageInformationDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPageService {

    private final UserRepository userRepository;
    private final JwtTokenizer jwtTokenizer;
    private final MyPageService myPageService;

    // 사용자 조회(userId로 조회)
    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        log.info("findUserById 실행");
        return userRepository.findById(userId).orElse(null);
    }

    // 사용자 조회(nickname으로 조회)
    @Transactional(readOnly = true)
    public User findUserByNickname(String nickname) {
        log.info("findUserByNickname 실행");
        return userRepository.findByNickname(nickname).orElse(null);
    }

    // accessToken 토큰으로 사용자 찾기
    @Transactional(readOnly = true)
    public User findByAccessToken(HttpServletRequest httpServletRequest) {
        log.info("findByAccessToken 실행");
        String accessToken = null;

        // accessTokne 값 추출
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                }
            }
        }
        // accessToken값으로  UserId 추출
        Long userId = Long.valueOf(
            ((Integer) jwtTokenizer.parseAccessToken(accessToken).get("userId")));
        // id(pk)에 해당되는 사용자 추출
        return findUserById(userId);
    }


    // 유저 페이지 정보 불러오기 응답 dto 구성
    @Transactional(readOnly = true)
    public UserPageInformationDto createUserPageInformationDto(String nickname) {
        User user = findUserByNickname(nickname);
        if (user != null) {
            log.info("UserPageInformationDto 실행 >> 해당 유저가 존재하여 UserPageInformationDto를 구성");
            Profile profile = user.getProfile(); //프로필 객체
            ProfileImages profileImages = profile.getProfileImages(); // 프로필 이미지 객체
            String fileUrl; // 프로필  URL

            // 기존 게시물 리스트에서 필요한 데이터만(이미지) 담은 postDto 리스트로 변환
            List<PostDto> postsList = user.getPostList().stream()
                // TempStatus가 TEMP가 아닌것만 필터링(임시저장글 제외)
                .filter(post -> post.getTempStatus() != TempStatus.TEMP)
                // 한 포스트에 쓰인 사진 리스트 중 첫번째 사진을 썸네일로 설정하여 해당 파일의 경로 사용
                .map(post -> {
                    String filePath = post.getPostImages().isEmpty() ? null // 이미지가 없을 경우 null 저장
                        : post.getPostImages().getFirst().getFilepath();
                    return new PostDto(post.getPostId(), filePath);
                })
                .collect(Collectors.toList());

            // 게시물 리스트가 비어있을때
            if (postsList.isEmpty()) {
                postsList = null;
            }

            UserPageInformationDto userPageInformationDto = UserPageInformationDto.builder()
                .nickname(user.getNickname()) // 닉네임
                .postsList(postsList) // 유저의 게시물 리스트
                .petName(profile.getPetName()) // 반려동물 이름
                .petSex(profile.getPetSex()) // 반려동물 성별
                .petAge(profile.getPetAge()) // 반려동물 나이
                .intro(profile.getIntro()) // 소개글
                .fileUrl(myPageService.profileFileUrl(profile.getProfileImages())) // 프로필 파일 경로
                .build();

            return userPageInformationDto;

        } else {
            log.info("UserPageInformationDto 실행 >> 해당 유저가 존재하지 않아 NOT_FOUND_USERPAGE 예외 발생");
            throw new UserPageNotFound();
        }

    }
}