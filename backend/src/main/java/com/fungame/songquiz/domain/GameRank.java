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

    public void deactivate(String player) {
        activePlayers.remove(player);
    }

    public void activate(String player) {
        if (scores.containsKey(player)) {
            activePlayers.add(player);
        }
    }

    public boolean hasPlayer(String player) {
        return activePlayers.contains(player);
    }

    public boolean hasParticipant(String player) {
        return scores.containsKey(player);
    }
}
