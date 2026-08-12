package com.fungame.songquiz.storage;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(
        name = "game_room_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_game_room_member_nickname",
                columnNames = {"game_room_id", "nickname"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRoomMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_room_id", nullable = false)
    private GameRoomEntity gameRoom;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false)
    private boolean ready;

    @Column(nullable = false)
    private Instant joinedAt;

    private GameRoomMemberEntity(String nickname, boolean ready) {
        this.nickname = nickname;
        this.ready = ready;
        this.joinedAt = Instant.now();
    }

    public static GameRoomMemberEntity of(String nickname, boolean ready) {
        return new GameRoomMemberEntity(nickname, ready);
    }

    void belongTo(GameRoomEntity gameRoom) {
        this.gameRoom = gameRoom;
    }

    void changeReady(boolean ready) {
        this.ready = ready;
    }
}
