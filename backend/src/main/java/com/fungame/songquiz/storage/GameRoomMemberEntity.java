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
                name = "uk_game_room_member_member",
                columnNames = {"game_room_id", "member_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRoomMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_room_id", nullable = false)
    private GameRoomEntity gameRoom;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private boolean ready;

    @Column(nullable = false)
    private Instant joinedAt;

    private GameRoomMemberEntity(Long memberId, boolean ready) {
        this.memberId = memberId;
        this.ready = ready;
        this.joinedAt = Instant.now();
    }

    public static GameRoomMemberEntity of(Long memberId, boolean ready) {
        return new GameRoomMemberEntity(memberId, ready);
    }

    void belongTo(GameRoomEntity gameRoom) {
        this.gameRoom = gameRoom;
    }

    void changeReady(boolean ready) {
        this.ready = ready;
    }
}
