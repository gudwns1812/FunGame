package com.fungame.songquiz.storage;

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

    @Column(nullable = false, length = 100)
    private String hostNickname;

    @Enumerated(EnumType.STRING)
    private Category category;

    private int totalRound;

    private int difficulty;

    @Column(nullable = false)
    private Instant lastActivityTime;

    @OneToMany(mappedBy = "gameRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameRoomMemberEntity> members = new ArrayList<>();

    private GameRoomEntity(RoomSettings settings, String hostNickname) {
        this.title = settings.title();
        this.gameType = settings.gameType();
        this.status = GameRoomStatus.WAITING;
        this.maxPlayer = settings.maxPlayers();
        this.hostNickname = hostNickname;
        this.category = settings.category();
        this.totalRound = settings.totalRound();
        this.difficulty = settings.difficulty();
        this.lastActivityTime = Instant.now();
    }

    public static GameRoomEntity open(RoomSettings settings, String hostNickname) {
        return new GameRoomEntity(settings, hostNickname);
    }

    public RoomSettings toSettings() {
        return new RoomSettings(gameType, title, maxPlayer, category, totalRound, difficulty);
    }

    public void applySettings(RoomSettings settings) {
        this.title = settings.title();
        this.maxPlayer = settings.maxPlayers();
        this.category = settings.category();
        this.totalRound = settings.totalRound();
        this.difficulty = settings.difficulty();
    }

    public void changeStatus(GameRoomStatus status) {
        this.status = status;
    }

    public void changeHost(String hostNickname) {
        this.hostNickname = hostNickname;
    }

    public void touch(Instant at) {
        this.lastActivityTime = at;
    }

    public void syncMembers(List<GamePlayer> players) {
        Map<String, GamePlayer> desiredByNickname = players.stream()
                .collect(Collectors.toMap(GamePlayer::name, player -> player));

        members.removeIf(existing -> !desiredByNickname.containsKey(existing.getNickname()));
        members.forEach(existing -> existing.changeReady(desiredByNickname.get(existing.getNickname()).isReady()));

        Set<String> alreadyJoined = members.stream()
                .map(GameRoomMemberEntity::getNickname)
                .collect(Collectors.toSet());

        players.stream()
                .filter(player -> !alreadyJoined.contains(player.name()))
                .forEach(player -> {
                    GameRoomMemberEntity member = GameRoomMemberEntity.of(player.name(), player.isReady());
                    member.belongTo(this);
                    members.add(member);
                });
    }
}
