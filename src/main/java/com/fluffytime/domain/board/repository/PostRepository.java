package com.fluffytime.domain.board.repository;

import com.fluffytime.domain.board.entity.Post;
import com.fluffytime.domain.board.entity.enums.TempStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findAllByUser_UserIdAndTempStatus(Long userId, TempStatus tempStatus);
}
