package com.fungame.songquiz.domain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRank {
    private final Map<String, Integer> scores;

    public GameRank(List<String> players) {
        this.scores = new ConcurrentHashMap<>();
        players.forEach(player -> scores.put(player, 0));
    }

    public void updatePoint(String player) {
        scores.merge(player, 1, Integer::sum);
    }

    public int getPlayerPoint(String player) {
        return scores.get(player);
    }

    public List<PlayerScore> getPlayerScores() {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new PlayerScore(entry.getKey(), entry.getValue()))
                .toList();
    }

    public void addPlayer(String player) {
        scores.put(player, 0);
    }

    public void removePlayer(String player) {
        scores.remove(player);
    }
}
