package com.fluffytime.domain.board.service;

import com.fluffytime.domain.board.exception.PostNotInTempStatus;
import com.fluffytime.domain.user.entity.Profile;
import com.fluffytime.global.auth.jwt.util.JwtTokenizer;
import com.fluffytime.global.common.exception.global.PostNotFound;
import com.fluffytime.global.common.exception.global.UserNotFound;
import com.fluffytime.global.config.aws.S3Service;
import com.fluffytime.domain.board.entity.Post;
import com.fluffytime.domain.board.entity.PostImages;
import com.fluffytime.domain.board.entity.enums.TempStatus;
import com.fluffytime.domain.user.entity.User;
import com.fluffytime.domain.board.dto.request.PostRequest;
import com.fluffytime.domain.board.dto.response.PostResponse;
import com.fluffytime.domain.board.exception.ContentLengthExceeded;
import com.fluffytime.domain.board.exception.FileSizeExceeded;
import com.fluffytime.domain.board.exception.FileUploadFailed;
import com.fluffytime.domain.board.exception.TooManyFiles;
import com.fluffytime.domain.board.exception.UnsupportedFileFormat;
import com.fluffytime.domain.board.repository.PostImagesRepository;
import com.fluffytime.domain.board.repository.PostRepository;
import com.fluffytime.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostImagesRepository postImagesRepository;
    private final JwtTokenizer jwtTokenizer;
    private final S3Service s3Service;
    private final TagService tagService;

    // 게시글 등록하기
    @Transactional
    public Long createPost(PostRequest postRequest, MultipartFile[] files,
        HttpServletRequest request) {
        // 업로드된 파일들의 유효성을 검증함
        validateFiles(files);

        User user = findUserByAccessToken(request);
        Post post;

        if (postRequest.getTempId() != null) {
            // 임시 저장된 글을 가져옴
            post = postRepository.findById(postRequest.getTempId())
                .orElseThrow(PostNotFound::new);

            // 상태 검증
            if (post.getTempStatus() != TempStatus.TEMP) {
                throw new PostNotInTempStatus();  // 상태가 올바르지 않으면 예외 발생
            }

            // 상태를 최종 등록으로 업데이트
            post.setTempStatus(TempStatus.SAVE);
            post.setUpdatedAt(LocalDateTime.now());
            post.setContent(postRequest.getContent());
        } else {
            // 새 게시물 생성
            post = Post.builder()
                .user(user)
                .content(postRequest.getContent())
                .createdAt(LocalDateTime.now())
                .tempStatus(TempStatus.SAVE)  // 새로 생성되는 게시물은 최종 등록 상태로 설정
                .build();
            postRepository.save(post);
        }

        // 이미지 저장 로직
        if (files != null && files.length > 0) {
            savePostImages(files, post);
        }

        // 태그 등록 로직
        tagService.regTags(postRequest.getTags(), post);

        return post.getPostId();  // 생성된 게시물의 ID를 반환
    }

    // 임시 게시글 등록하기
    @Transactional
    public Long createTempPost(PostRequest postRequest, MultipartFile[] files,
        HttpServletRequest request) {
        // 업로드된 파일들의 유효성을 검증함
        validateFiles(files);

        User user = findUserByAccessToken(request);

        // 임시 게시물을 생성함
        Post post = Post.builder()
            .user(user)
            .content(postRequest.getContent())
            .createdAt(LocalDateTime.now())
            .tempStatus(TempStatus.TEMP)
            .build();

        postRepository.save(post);

        // 태그 등록 로직
        tagService.regTags(postRequest.getTags(), post);

        if (files != null && files.length > 0) {
            savePostImages(files, post);
        }

        return post.getPostId(); // 생성된 임시 게시물의 ID를 반환
    }

    // 이미지 파일 저장 로직
    private void savePostImages(MultipartFile[] files, Post post) {
        for (MultipartFile file : files) {
            try {
                // 이미지를 S3에 업로드하고 URL을 가져옴
                String fileName = s3Service.uploadFile(file);
                String fileUrl = s3Service.getFileUrl(fileName);

                // PostImages 엔티티를 생성하여 데이터베이스에 저장함
                PostImages postImage = PostImages.builder()
                    .filename(fileName)
                    .filepath(fileUrl)
                    .filesize(file.getSize())
                    .mimetype(file.getContentType())
                    .post(post)
                    .build();

                postImagesRepository.save(postImage);
            } catch (Exception e) {
                throw new FileUploadFailed();
            }
        }
    }

    // 게시글 조회하기
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id, Long currentUserId) {
        // 게시글을 조회하고, 없으면 예외를 발생시킴
        Post post = postRepository.findById(id)
            .orElseThrow(PostNotFound::new);

        return convertToPostResponse(post, currentUserId);
    }

    // 게시글 수정하기
    @Transactional
    public PostResponse updatePost(Long id, PostRequest postRequest, MultipartFile[] files,
        HttpServletRequest request, Long currentUserId) {
        // 토큰을 통해 사용자 정보 추출
        User user = findUserByAccessToken(request);

        Post existingPost = postRepository.findById(id)
            .orElseThrow(PostNotFound::new);

        // 게시물 소유자인지 확인
        if (!existingPost.getUser().equals(user)) {
            throw new UserNotFound(); // 권한이 없으면 NotFoundUser 예외 발생
        }

        // 게시물 내용의 길이를 검증함
        if (postRequest.getContent() != null && postRequest.getContent().length() > 2200) {
            throw new ContentLengthExceeded();
        }

        // 게시물 내용을 업데이트함
        existingPost.setContent(postRequest.getContent());

        // 새로운 파일이 업로드된 경우 이미지를 저장함
        if (files != null && files.length > 0) {
            savePostImages(files, existingPost);
        }

        existingPost.setUpdatedAt(LocalDateTime.now());
        postRepository.save(existingPost);

        return convertToPostResponse(existingPost, currentUserId);
    }

    // 게시글 삭제하기
    @Transactional
    public void deletePost(Long id, HttpServletRequest request) {
        // 토큰을 통해 사용자 정보 추출
        User user = findUserByAccessToken(request);

        Post post = postRepository.findById(id)
            .orElseThrow(PostNotFound::new);

        // 게시물 소유자인지 확인
        if (!post.getUser().equals(user)) {
            throw new UserNotFound();
        }

        postRepository.deleteById(id);
    }

    // 임시 게시글 삭제하기
    @Transactional
    public void deleteTempPost(Long id) {
        Post post = postRepository.findById(id)
            .orElseThrow(PostNotFound::new);

        // 임시 저장된 상태인 경우에만 삭제함
        if (post.getTempStatus() == TempStatus.TEMP) {
            postRepository.deleteById(id);
            log.info("게시물 ID {}가 성공적으로 삭제되었습니다.", id);
        } else {
            throw new PostNotInTempStatus();
        }
    }

    // 임시 게시글 목록 조회하기
    @Transactional(readOnly = true)
    public List<PostResponse> getTempPosts(Long currentUserId) {
        // 모든 게시글을 조회한 후, 임시 저장된 게시물만 필터링함
        List<Post> tempPosts = postRepository.findAll().stream()
            .filter(post -> post.getTempStatus() == TempStatus.TEMP)
            .collect(Collectors.toList());

        return tempPosts.stream()
            .map(post -> convertToPostResponse(post, currentUserId))
            .collect(Collectors.toList());
    }

    // 파일 검증 로직
    private void validateFiles(MultipartFile[] files) {
        if (files == null) {
            return;
        }

        checkFileCount(files);
        for (MultipartFile file : files) {
            checkFileSize(file);
            checkFileFormat(file);
        }
    }

    private void checkFileCount(MultipartFile[] files) {
        if (files.length > 10) {
            throw new TooManyFiles();
        }
    }

    private void checkFileSize(MultipartFile file) {
        if (file.getSize() > 10485760) {
            throw new FileSizeExceeded();
        }
    }

    private void checkFileFormat(MultipartFile file) {
        if (!isSupportedFormat(file.getContentType())) {
            throw new UnsupportedFileFormat();
        }
    }

    // 지원되는 파일 형식인지 확인
    private boolean isSupportedFormat(String contentType) {
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals(
            "image/png"));
    }

    // jwtTokenizer.getTokenFromCookie를 통해 토큰 추출
    @Transactional
    public User findUserByAccessToken(HttpServletRequest httpServletRequest) {
        log.info("findUserByAccessToken 실행");

        String accessToken = jwtTokenizer.getTokenFromCookie(httpServletRequest, "accessToken");

        if (accessToken == null) {
            throw new UserNotFound();  // 토큰이 없으면 사용자 정보를 찾을 수 없음
        }

        Long userId = jwtTokenizer.getUserIdFromToken(accessToken);
        return userRepository.findById(userId).orElseThrow(UserNotFound::new);
    }

    // Post 엔티티를 PostResponse로 변환하는 메소드
    private PostResponse convertToPostResponse(Post post, Long currentUserId) {
        // 작성자(User) 정보 가져오기
        User author = post.getUser();
        Profile profile = author.getProfile(); // 작성자의 프로필 정보 가져오기

        // Profile이 존재할 경우에만 관련 정보를 가져옴
        String profileImageUrl = profile != null && profile.getProfileImages() != null ? profile.getProfileImages().getFilePath() : null;
        String petName = profile != null ? profile.getPetName() : null;
        String petSex = profile != null ? profile.getPetSex() : null;
        Long petAge = profile != null ? profile.getPetAge() : null;

        return new PostResponse(
            post.getPostId(),
            post.getContent(),
            post.getPostImages().stream().map(image -> new PostResponse.ImageResponse(
                image.getImageId(),
                image.getFilename(),
                image.getFilepath(),
                image.getFilesize(),
                image.getMimetype(),
                image.getDescription(),
                image.getUploadDate().format(DateTimeFormatter.ISO_DATE_TIME)
            )).collect(Collectors.toList()),
            post.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME),
            post.getUpdatedAt() != null ? post.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null,
            post.getLikes().size(),
            post.getLikes().stream().anyMatch(like -> like.getUser().getUserId().equals(currentUserId)),
            post.isCommentsDisabled(),
            author.getNickname(),        // 작성자 닉네임
            profileImageUrl,             // 프로필 이미지 URL
            petName,                     // 반려동물 이름
            petSex,                      // 반려동물 성별
            petAge                       // 반려동물 나이
        );
    }

    @Transactional
    public void toggleComments(Long postId, User user) {
        Post post = postRepository.findById(postId)
            .orElseThrow(PostNotFound::new);

        // 요청한 사용자가 게시글 작성자인지 확인
        if (!post.getUser().getUserId().equals(user.getUserId())) {
            throw new UserNotFound(); // 권한이 없으면 UserNotFound 예외 발생
        }

        // 댓글 기능 상태를 토글
        post.setCommentsDisabled(!post.isCommentsDisabled());
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public boolean checkIfUserIsAuthor(Long postId, User user) {
        Post post = postRepository.findById(postId)
            .orElseThrow(PostNotFound::new);

        return post.getUser().getUserId().equals(user.getUserId());
    }


}