package com.fluffytime.bookmark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookmarkResponse {

    private Long bookmarkId;
    private Long userId;
    private Long postId;
}