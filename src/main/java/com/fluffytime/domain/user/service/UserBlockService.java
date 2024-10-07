package com.fluffytime.domain.user.service;

import com.fluffytime.domain.user.dao.UserBlockDao;
import com.fluffytime.domain.user.dto.response.BlockUserListResponse;
import com.fluffytime.domain.user.dto.response.UserBlockResponse;
import com.fluffytime.domain.user.entity.Profile;
import com.fluffytime.domain.user.entity.User;
import com.fluffytime.global.auth.jwt.exception.TokenNotFound;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserBlockService {

    private final MyPageService mypageService;
    private final UserBlockDao  userBlockDao;

    // 유저 차단  메서드
    @Transactional
    public UserBlockResponse userBlock(String targetUser,
        HttpServletRequest httpServletRequest) {
        log.info("userBlock 실행");
        String blockerId = Long.toString(mypageService.findByAccessToken(httpServletRequest).getUserId()); // 차단 하는 유저
        String blockedId = Long.toString(mypageService.findUserByNickname(targetUser).getUserId()); // 차단 당하는 유저

        UserBlockResponse userBlockResponse = new UserBlockResponse(true);

        userBlockDao.saveUserBlockList(blockerId, blockedId);
        return userBlockResponse;
    }

    // 유저 차단 여부  메서드
    public boolean isUserBlocked(Long blockerId, Long targetUserId) {
        log.info("isUserBlocked 실행");
        Set<String> blockedUsers = userBlockDao.getUserBlockList(Long.toString(blockerId));
        return blockedUsers.contains(Long.toString(targetUserId));
    }

    // 유저 차단 해제  메서드
    @Transactional
    public UserBlockResponse removeUserBlock(String targetUser,
        HttpServletRequest httpServletRequest) {
        log.info("removeUserBlock 실행");

        String blockerId = Long.toString(mypageService.findByAccessToken(httpServletRequest).getUserId()); // 차단 하는 유저
        String blockedId = Long.toString(mypageService.findUserByNickname(targetUser).getUserId());// 차단 당하는 유저
        UserBlockResponse userBlockResponse = new UserBlockResponse(true);

        userBlockDao.removeUserBlockList(blockerId, blockedId);

        return userBlockResponse;
    }

    // 유저 차단 목록 생성  메서드
    @Transactional
    public List<Map<String, String>> BlockList(Set<String> blockedUsers) {
        log.info("blockedUsers 실행");
        // 사용자명 - 프로필 사진 url를 담을 리스트
        List<Map<String, String>> userBlockList = new ArrayList<>();

        // 리스트에 목록 추가하기
        for (String userId : blockedUsers) {
            // 차단 유저 객체
            User user = mypageService.findUserById(Long.parseLong(userId));
            // 프로필 객체 찾기
            Profile profile = user.getProfile();

            // 프로필 사진 url 찾기
            String fileUrl = mypageService.profileFileUrl(profile.getProfileImages());

            // 닉네임과 프로필 URL을 매칭하여 맵에 저장
            Map<String, String> blockUserMap = new HashMap<>();
            blockUserMap.put("nickname", user.getNickname());
            blockUserMap.put("fileUrl", fileUrl);
            // 리스트에 추가
            userBlockList.add(blockUserMap);
        }
        return userBlockList;
    }

    // 유저 차단 목록 가져오기  메서드
    @Transactional
    public BlockUserListResponse blockUserList(HttpServletRequest httpServletRequest) {
        log.info("blockUserList 실행");
        String blockerId = Long.toString(mypageService.findByAccessToken(httpServletRequest).getUserId());

        // 차단된 사용자 리스트 가져오기
        Set<String> blockedUsers = userBlockDao.getUserBlockList(blockerId);

        return new BlockUserListResponse(BlockList(blockedUsers));
    }
}
