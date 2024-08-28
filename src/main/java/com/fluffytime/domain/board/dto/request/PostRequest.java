package com.fluffytime.domain.board.dto.request;

import com.fluffytime.domain.board.entity.enums.TempStatus;
import com.fluffytime.domain.board.validation.annotation.ValidTags;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PostRequest {

    private Long tempId; // 임시 저장된 글 ID
    private String content;
    @ValidTags
    private List<String> tags;
    private TempStatus tempStatus;

}
