package com.fungame.songquiz.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameRank {
    private final Map<String, Integer> scores;
    private final Set<String> activePlayers;

    public GameRank(List<String> players) {
        this.scores = new ConcurrentHashMap<>();
        this.activePlayers = ConcurrentHashMap.newKeySet();
        players.forEach(this::addPlayer);
    }

    public void updatePoint(String player) {
        scores.merge(player, 1, Integer::sum);
    }

    public List<PlayerScore> getPlayerScores() {
        return scores.entrySet().stream()
                .filter(entry -> activePlayers.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new PlayerScore(entry.getKey(), entry.getValue()))
                .toList();
    }

    public void addPlayer(String player) {
        scores.put(player, 0);
        activePlayers.add(player);
    }

    /**
     * 이탈 처리. 점수는 남겨두고 순위에서만 제외한다.
     * 재입장했을 때 이전 점수를 그대로 이어받기 위함이다.
     */
    public void deactivate(String player) {
        activePlayers.remove(player);
    }

    /**
     * 재입장 처리. 이 게임의 참가자였던 경우에만 순위에 다시 노출한다.
     */
    public void activate(String player) {
        if (scores.containsKey(player)) {
            activePlayers.add(player);
        }
    }

    public boolean hasPlayer(String player) {
        return activePlayers.contains(player);
    }

    /**
     * 지금 접속 중인지와 무관하게, 이 게임에 참가했던 사람인지 여부.
     */
    public boolean hasParticipant(String player) {
        return scores.containsKey(player);
    }
}
