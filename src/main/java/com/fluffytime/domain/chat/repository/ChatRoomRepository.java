package com.fluffytime.domain.chat.repository;

import com.fluffytime.domain.chat.entity.ChatRoom;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 채팅방이 존재하는지 여부
    boolean existsByRoomName(String roomName);

    //채팅방 이름으로 채팅방 객체 찾기
    Optional<ChatRoom> findByRoomName(String roodName);

    // 유저 아이디로 해당 유저가 속한 채팅방 가져오기
    @Query("SELECT c.roomName FROM chat_room c WHERE c.userId1 = :userId OR c.userId2 = :userId")
    Optional<Set<String>> findByRoomNameContaining(@Param("userId") Long userId);

    // 유저 아이디로 내가 속한 채팅방들 중에서 나와 채팅하는 모든 유저 가져오기
    @Query(
        "SELECT CASE WHEN c.userId1 = :userId THEN c.userId2 ELSE c.userId1 END " +
            "FROM chat_room c " +
            "WHERE c.userId1 = :userId OR c.userId2 = :userId")
    Optional<Set<Long>> findAllOtherParticipants(@Param("userId") Long userId);



}
