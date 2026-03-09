package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;

public class GamePlayers {
    private final Map<String, GamePlayer> players;
    @Getter
    private final int maxPlayer;
    @Getter
    private String host;

    public GamePlayers(List<String> players, int maxPlayer, String host) {
        this.players = players.stream()
                .map(GamePlayer::createNewPlayer)
                .collect(Collectors.toMap(GamePlayer::name, player -> player , (existing, replacement) -> existing, LinkedHashMap::new));
        this.maxPlayer = maxPlayer;
        this.host = host;
        
        // 방장은 항상 준비 상태여야 함
        if (this.players.containsKey(host)) {
            this.players.put(host, this.players.get(host).ready());
        }
    }

    public void add(String player) {
        if (isFull()) {
            throw new CoreException(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
        }

        if (players.get(player) != null) {
            return;
        }

        players.put(player, GamePlayer.createNewPlayer(player));
    }

    public void remove(String player) {
        players.remove(player);

        if (player.equals(host) && !players.isEmpty()) {
            delegateHost();
        }
    }

    private void delegateHost() {
        if (!players.isEmpty()) {
            host = players.values().stream()
                    .findFirst()
                    .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_PLAYER_EMPTY))
                    .name();
            
            // 새 방장도 즉시 준비 상태로 변경
            players.put(host, players.get(host).ready());
        }
    }

    public boolean isFull() {
        return players.size() >= maxPlayer;
    }

    public List<String> getPlayers() {
        return players.values().stream().map(GamePlayer::name)
                .toList();
    }

    public int getCurrentCount() {
        return players.size();
    }

    public void readyPlayer(String player) {
        if (!players.containsKey(player)) {
            throw new CoreException(ErrorType.PLAYER_NOT_FOUND);
        }

        players.put(player, players.get(player).ready());
    }

    public boolean isAllReady() {
        // 모든 인원이 isReady 상태여야 함 (방장은 생성/위임 시 항상 true로 보장됨)
        return players.values().stream()
                .allMatch(GamePlayer::isReady);
    }
}
