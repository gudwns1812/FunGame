package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GamePlayerInfo;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GamePlayers {
    private final Map<Long, GamePlayer> players;
    @Getter
    private int maxPlayer;
    @Getter
    private Long host;

    public GamePlayers(List<GamePlayer> players, int maxPlayer, Long host) {
        this.players = toPlayerMap(players);
        this.maxPlayer = maxPlayer;
        this.host = host;

        // 방장은 항상 준비 상태여야 함
        if (this.players.containsKey(host)) {
            this.players.put(host, this.players.get(host).setReady(true));
        }
    }

    private static Map<Long, GamePlayer> toPlayerMap(List<GamePlayer> players) {
        return players.stream()
                .collect(Collectors.toMap(GamePlayer::memberId, player -> player, (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    public List<GamePlayer> snapshot() {
        return List.copyOf(players.values());
    }

    public void changeMaxPlayer(int maxPlayer) {
        if (maxPlayer < players.size()) {
            throw new CoreException(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
        }
        this.maxPlayer = maxPlayer;
    }

    public void resetReady() {
        players.replaceAll((memberId, player) -> player.setReady(memberId.equals(host)));
    }

    public boolean add(GamePlayer player) {
        if (isAlreadyIn(player.memberId())) {
            return false;
        }

        if (isFull()) {
            throw new CoreException(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
        }

        players.put(player.memberId(), player);
        return true;
    }

    private boolean isAlreadyIn(Long memberId) {
        return players.containsKey(memberId);
    }

    public void remove(Long memberId) {
        players.remove(memberId);

        if (memberId.equals(host) && !players.isEmpty()) {
            delegateHost();
        }
    }

    private void delegateHost() {
        players.values().stream()
                .findFirst()
                .map(GamePlayer::memberId)
                .ifPresent(this::assignHost);
    }

    private void assignHost(Long memberId) {
        host = memberId;
        // 새 방장도 즉시 준비 상태로 변경
        players.put(memberId, players.get(memberId).setReady(true));
    }

    public boolean isFull() {
        return players.size() >= maxPlayer;
    }

    public List<GamePlayer> getPlayers() {
        return List.copyOf(players.values());
    }

    public boolean hasPlayer(Long memberId) {
        return players.containsKey(memberId);
    }

    public String nicknameOf(Long memberId) {
        GamePlayer player = players.get(memberId);
        return player == null ? null : player.nickname();
    }

    public List<GamePlayerInfo> getPlayersWithReadyStatus() {
        return players.values().stream()
                .map(GamePlayerInfo::from)
                .toList();
    }

    public int getCurrentCount() {
        return players.size();
    }

    public boolean readyPlayer(Long memberId) {
        if (!players.containsKey(memberId)) {
            throw new CoreException(ErrorType.PLAYER_NOT_FOUND);
        }

        // 방장은 준비 해제 불가, 항상 true
        if (memberId.equals(host)) {
            players.put(memberId, players.get(memberId).setReady(true));
        } else {
            players.put(memberId, players.get(memberId).toggleReady());
        }

        return players.get(memberId).isReady();
    }

    public boolean isAllReady() {
        return players.values().stream()
                .allMatch(GamePlayer::isReady);
    }
}
