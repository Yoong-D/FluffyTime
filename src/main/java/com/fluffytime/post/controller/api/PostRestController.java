package com.fluffytime.post.controller.api;

import com.fluffytime.post.dto.request.PostRequest;
import com.fluffytime.post.dto.response.PostResponse;
import com.fluffytime.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostRestController {

    private final PostService postService;

    // 게시물 등록
    @PostMapping("/reg")
    public ResponseEntity<Long> regPost(@RequestPart("post") PostRequest postRequest,
        @RequestPart(value = "images", required = false) MultipartFile[] files,
        HttpServletRequest request) {
        log.info("게시물 등록 요청 받음: {}", postRequest);

        if (postRequest.getTempId() != null) {
            // 임시 저장된 글 최종 등록 시 이미지 추가/수정 불가
            files = null;
        }

        Long postId = postService.createPost(postRequest, files, request);

        if (postRequest.getTempId() != null) {
            postService.deleteTempPost(postRequest.getTempId());
        }

        log.info("게시물 등록 성공, ID: {}", postId);
        return ResponseEntity.status(HttpStatus.OK).body(postId);
    }

    // 임시 게시물 등록
    @PostMapping("/temp-reg")
    public ResponseEntity<Long> tempRegPost(@RequestPart("post") PostRequest postRequest,
        @RequestPart("images") MultipartFile[] files,
        HttpServletRequest request) {
        log.info("임시 게시물 등록 요청 받음: {}", postRequest);

        Long postId = postService.createTempPost(postRequest, files, request);

        log.info("임시 게시물 등록 성공, ID: {}", postId);
        return ResponseEntity.status(HttpStatus.OK).body(postId);
    }

    // 임시 게시물 삭제
    @PostMapping("/temp-delete/{id}")
    public ResponseEntity<Void> deleteTempPost(@PathVariable(name = "id") Long id) {
        log.info("임시 게시물 삭제 요청 받음, ID: {}", id);
        postService.deleteTempPost(id);
        log.info("임시 게시물 삭제 성공, ID: {}", id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 임시 게시물 목록 조회
    @GetMapping("/temp-posts/list")
    public ResponseEntity<List<PostResponse>> getTempPosts() {
        log.info("임시 게시물 목록 조회 요청 받음");
        List<PostResponse> tempPosts = postService.getTempPosts();
        log.info("임시 게시물 목록 조회 성공, 개수: {}", tempPosts.size());
        return ResponseEntity.status(HttpStatus.OK).body(tempPosts);
    }

    // 게시물 상세 정보 조회
    @GetMapping("/detail/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable(name = "id") Long id) {
        log.info("게시물 상세 정보 조회 요청 받음, ID: {}", id);
        PostResponse postResponse = postService.getPostById(id);
        log.info("게시물 상세 정보 조회 성공, ID: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(postResponse);
    }

    // 게시물 수정
    @PostMapping("/edit/{id}")
    public ResponseEntity<PostResponse> editPost(@PathVariable(name = "id") Long id,
        @RequestPart(value = "post") PostRequest postRequest,
        @RequestPart(value = "files", required = false) MultipartFile[] files,
        HttpServletRequest request) {
        log.info("게시물 수정 요청 받음, ID: {}", id);

        PostResponse updatedPostResponse = postService.updatePost(id, postRequest, files, request);
        log.info("게시물 수정 성공, ID: {}", updatedPostResponse.getPostId());
        return ResponseEntity.status(HttpStatus.OK).body(updatedPostResponse);
    }

    // 게시물 삭제
    @PostMapping("/delete/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable(name = "id") Long id,
        HttpServletRequest request) {
        log.info("게시물 삭제 요청 받음, ID: {}", id);

        postService.deletePost(id, request);
        log.info("게시물 삭제 성공, ID: {}", id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
