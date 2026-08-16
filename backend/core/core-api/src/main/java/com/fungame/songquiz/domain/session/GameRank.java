package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.room.GamePlayer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameRank {
    private final Map<Long, Integer> scores;
    private final Map<Long, String> nicknames;
    private final Set<Long> activePlayers;

    public GameRank(List<GamePlayer> players) {
        this.scores = new ConcurrentHashMap<>();
        this.nicknames = new ConcurrentHashMap<>();
        this.activePlayers = ConcurrentHashMap.newKeySet();
        players.forEach(this::addPlayer);
    }

    public void updatePoint(Long memberId) {
        scores.merge(memberId, 1, Integer::sum);
    }

    public List<PlayerScore> getPlayerScores() {
        return scores.entrySet().stream()
                .filter(entry -> activePlayers.contains(entry.getKey()))
                .map(entry -> new PlayerScore(entry.getKey(), nicknameOf(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingInt(PlayerScore::score).reversed()
                        .thenComparing(PlayerScore::nickname))
                .toList();
    }

    public void addPlayer(GamePlayer player) {
        scores.put(player.memberId(), 0);
        nicknames.put(player.memberId(), player.nickname());
        activePlayers.add(player.memberId());
    }

    public String nicknameOf(Long memberId) {
        return nicknames.get(memberId);
    }

    public void deactivate(Long memberId) {
        activePlayers.remove(memberId);
    }

    public void activate(GamePlayer player) {
        if (scores.containsKey(player.memberId())) {
            nicknames.put(player.memberId(), player.nickname());
            activePlayers.add(player.memberId());
        }
    }

    public boolean hasPlayer(Long memberId) {
        return activePlayers.contains(memberId);
    }

    public int playerCount() {
        return activePlayers.size();
    }

    public boolean hasParticipant(Long memberId) {
        return scores.containsKey(memberId);
    }
}
