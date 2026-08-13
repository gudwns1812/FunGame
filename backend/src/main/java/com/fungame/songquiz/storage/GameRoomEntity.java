package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GamePlayer;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.RoomSettings;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Table(name = "game_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameRoomStatus status;

    @Column(nullable = false)
    private int maxPlayer;

    @Column(nullable = false)
    private Long hostMemberId;

    @Enumerated(EnumType.STRING)
    private Category category;

    private int totalRound;

    private int difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CSQuizDifficulty csDifficulty;

    @Column(nullable = false)
    private Instant lastActivityTime;

    @OneToMany(mappedBy = "gameRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameRoomMemberEntity> members = new ArrayList<>();

    private GameRoomEntity(RoomSettings settings, Long hostMemberId) {
        this.title = settings.title();
        this.gameType = settings.gameType();
        this.status = GameRoomStatus.WAITING;
        this.maxPlayer = settings.maxPlayers();
        this.hostMemberId = hostMemberId;
        this.category = settings.category();
        this.totalRound = settings.totalRound();
        this.difficulty = settings.difficulty();
        this.csDifficulty = settings.csDifficulty();
        this.lastActivityTime = Instant.now();
    }

    public static GameRoomEntity open(RoomSettings settings, Long hostMemberId) {
        return new GameRoomEntity(settings, hostMemberId);
    }

    public RoomSettings toSettings() {
        return new RoomSettings(gameType, title, maxPlayer, category, totalRound, difficulty, csDifficulty);
    }

    public void applySettings(RoomSettings settings) {
        this.title = settings.title();
        this.maxPlayer = settings.maxPlayers();
        this.category = settings.category();
        this.totalRound = settings.totalRound();
        this.difficulty = settings.difficulty();
        this.csDifficulty = settings.csDifficulty();
    }

    public void changeStatus(GameRoomStatus status) {
        this.status = status;
    }

    public void changeHost(Long hostMemberId) {
        this.hostMemberId = hostMemberId;
    }

    public void touch(Instant at) {
        this.lastActivityTime = at;
    }

    public void syncMembers(List<GamePlayer> players) {
        Map<Long, GamePlayer> desiredByMemberId = players.stream()
                .collect(Collectors.toMap(GamePlayer::memberId, player -> player));

        members.removeIf(existing -> !desiredByMemberId.containsKey(existing.getMemberId()));
        members.forEach(existing -> existing.changeReady(desiredByMemberId.get(existing.getMemberId()).isReady()));

        Set<Long> alreadyJoined = members.stream()
                .map(GameRoomMemberEntity::getMemberId)
                .collect(Collectors.toSet());

        players.stream()
                .filter(player -> !alreadyJoined.contains(player.memberId()))
                .forEach(player -> {
                    GameRoomMemberEntity member = GameRoomMemberEntity.of(player.memberId(), player.isReady());
                    member.belongTo(this);
                    members.add(member);
                });
    }
}
