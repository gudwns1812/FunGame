package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
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

    public record Settings(
            GameType gameType,
            String title,
            int maxPlayer,
            Category category,
            int totalRound,
            int difficulty,
            CSQuizDifficulty csDifficulty
    ) {
    }

    public record MemberState(Long memberId, boolean ready) {
    }

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

    private GameRoomEntity(Settings settings, Long hostMemberId) {
        this.status = GameRoomStatus.WAITING;
        this.hostMemberId = hostMemberId;
        this.lastActivityTime = Instant.now();
        applySettings(settings);
    }

    public static GameRoomEntity open(Settings settings, Long hostMemberId) {
        return new GameRoomEntity(settings, hostMemberId);
    }

    public Settings toSettings() {
        return new Settings(gameType, title, maxPlayer, category, totalRound, difficulty, csDifficulty);
    }

    public void applySettings(Settings settings) {
        this.gameType = settings.gameType();
        this.title = settings.title();
        this.maxPlayer = settings.maxPlayer();
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

    public void syncMembers(List<MemberState> desired) {
        Map<Long, MemberState> desiredByMemberId = desired.stream()
                .collect(Collectors.toMap(MemberState::memberId, state -> state));

        members.removeIf(existing -> !desiredByMemberId.containsKey(existing.getMemberId()));
        members.forEach(existing -> existing.changeReady(desiredByMemberId.get(existing.getMemberId()).ready()));

        Set<Long> alreadyJoined = members.stream()
                .map(GameRoomMemberEntity::getMemberId)
                .collect(Collectors.toSet());

        desired.stream()
                .filter(state -> !alreadyJoined.contains(state.memberId()))
                .forEach(state -> {
                    GameRoomMemberEntity member = GameRoomMemberEntity.of(state.memberId(), state.ready());
                    member.belongTo(this);
                    members.add(member);
                });
    }
}
