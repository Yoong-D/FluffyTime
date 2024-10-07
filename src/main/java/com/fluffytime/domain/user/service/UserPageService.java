package com.fluffytime.domain.user.service;

import com.fluffytime.domain.board.entity.Mention;
import com.fluffytime.domain.board.entity.enums.TempStatus;
import com.fluffytime.domain.board.repository.MentionRepository;
import com.fluffytime.domain.user.dao.UserBlockDao;
import com.fluffytime.domain.user.dto.response.PostResponse;
import com.fluffytime.domain.user.dto.response.UserPageInformationResponse;
import com.fluffytime.domain.user.entity.Profile;
import com.fluffytime.domain.user.entity.User;
import com.fluffytime.domain.user.exception.UserPageNotFound;
import com.fluffytime.global.auth.jwt.exception.TokenNotFound;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPageService {

    private final MyPageService mypageService;
    private final UserBlockService userBlockService;
    private final UserBlockDao userBlockDao;
    private final MentionRepository mentionRepository;


    // 기존 게시물 리스트에서 필요한 데이터만(이미지) 담은 postDto 리스트로 변환하는 메서드
    public List<PostResponse> postList(User user) {
        return user.getPostList().stream()
            // TempStatus가 TEMP가 아닌것만 필터링(임시저장글 제외)
            .filter(post -> post.getTempStatus() != TempStatus.TEMP)
            // 한 포스트에 쓰인 사진 리스트 중 첫번째 사진을 썸네일로 설정하여 해당 파일의 경로 사용
            .map(post -> {
                String filePath = post.getPostImages().isEmpty() ? null // 이미지가 없을 경우 null 저장
                    : post.getPostImages().getFirst().getFilepath();
                String mineType = post.getPostImages().isEmpty() ? null // 이미지가 없을 경우 null 저장
                    : post.getPostImages().getFirst().getMimetype();
                return new PostResponse(post.getPostId(), filePath, mineType);
            })
            .collect(Collectors.collectingAndThen(Collectors.toList(), list -> { // 역순
                Collections.reverse(list);
                return list;
            }));
    }

    // 태그된 게시물 리스트 메서드
    public List<PostResponse> tagePostList(List<Mention> mentions) {
        List<PostResponse> postResponses = mentions.stream()
            .map(Mention::getPost) // Mention에서 Post 객체를 가져옴
            .filter(Objects::nonNull) // Post가 null이 아닌 것만 처리
            .map(post -> {
                // 첫 번째 이미지의 파일 경로와 MIME 타입을 가져옴
                String filePath = post.getPostImages().isEmpty() ? null
                    : post.getPostImages().get(0).getFilepath();
                String mimeType = post.getPostImages().isEmpty() ? null
                    : post.getPostImages().get(0).getMimetype();

                // PostResponse 객체로 변환
                return new PostResponse(post.getPostId(), filePath, mimeType);
            })
            .collect(Collectors.toList());

        // 역순으로 정렬
        Collections.reverse(postResponses);

        return postResponses;
    }

    // UserPageInformationDto 생성 메서드
    public UserPageInformationResponse createResponseDto(User user, List<PostResponse> postsList,
        List<PostResponse> tagePostList,
        Profile profile, boolean isUserBlocked) {
        return UserPageInformationResponse.builder()
            .nickname(user.getNickname()) // 닉네임
            .postsList(postsList) // 유저의 게시물 리스트
            .tagePostList(tagePostList) // 태그된 게시물 리스트
            .petName(profile.getPetName()) // 반려동물 이름
            .petSex(profile.getPetSex()) // 반려동물 성별
            .petAge(profile.getPetAge()) // 반려동물 나이
            .intro(profile.getIntro()) // 소개글
            .fileUrl(mypageService.profileFileUrl(profile.getProfileImages())) // 프로필 파일 경로
            .publicStatus(profile.getPublicStatus()) // 프로필 공개 여부
            .isUserBlocked(isUserBlocked) // 해당 유저를 사용자가 차단 했는지 여부
            .build();
    }

    // 유저 페이지 정보 불러오기  메서드
    @Transactional
    public UserPageInformationResponse createUserPageInformationDto(String nickname,
        HttpServletRequest httpServletRequest) {
        User me = mypageService.findByAccessToken(httpServletRequest); // 현재 로그인한 유저
        User user = mypageService.findUserByNickname(nickname);

        if (me == null) {
            throw new TokenNotFound();
        }

        if (user != null) {
            log.info("UserPageInformationDto 실행 >> 해당 유저가 존재하여 UserPageInformationDto를 구성");
            Profile profile = user.getProfile(); //프로필 객체
            boolean isUserBlocked = userBlockService.isUserBlocked(me.getUserId(), user.getUserId()); // 차단 여부 반환

            // 기존 게시물 리스트에서 필요한 데이터만(이미지) 담은 postDto 리스트로 변환
            List<PostResponse> postsList = postList(user);

            // 멘션된 게시물 리스트
            List<Mention> postMentions = mentionRepository.findByMetionedUserAndPostIsNotNull(user);
            List<PostResponse> tagePostList = tagePostList(postMentions);

            // 게시물 리스트가 비어있을때
            if (postsList.isEmpty()) {
                postsList = null;
            }

            return createResponseDto(user, postsList, tagePostList, profile, isUserBlocked);

        } else {
            log.info("UserPageInformationDto 실행 >> 해당 유저가 존재하지 않아 NOT_FOUND_USERPAGE 예외 발생");
            throw new UserPageNotFound();
        }

    }


}
