package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.room.GamePlayer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRank {
    private final Map<Long, Participant> participants = new ConcurrentHashMap<>();

    public GameRank(List<GamePlayer> players) {
        players.forEach(this::addPlayer);
    }

    public void updatePoint(Long memberId) {
        participants.computeIfPresent(memberId, (id, participant) -> participant.scored());
    }

    public List<PlayerScore> getPlayerScores() {
        return participants.values().stream()
                .filter(Participant::playing)
                .map(Participant::toPlayerScore)
                .sorted(Comparator.comparingInt(PlayerScore::score).reversed()
                        .thenComparing(PlayerScore::nickname))
                .toList();
    }

    public void addPlayer(GamePlayer player) {
        participants.put(player.memberId(), Participant.joining(player));
    }

    public String nicknameOf(Long memberId) {
        Participant participant = participants.get(memberId);
        return participant == null ? null : participant.nickname();
    }

    public void deactivate(Long memberId) {
        participants.computeIfPresent(memberId, (id, participant) -> participant.left());
    }

    public void activate(GamePlayer player) {
        participants.computeIfPresent(player.memberId(), (id, participant) -> participant.returnedAs(player));
    }

    public boolean hasPlayer(Long memberId) {
        Participant participant = participants.get(memberId);
        return participant != null && participant.playing();
    }

    public boolean hasParticipant(Long memberId) {
        return participants.containsKey(memberId);
    }

    public int playerCount() {
        return (int) participants.values().stream()
                .filter(Participant::playing)
                .count();
    }
}
