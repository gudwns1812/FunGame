package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.room.GamePlayer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GameRank {
    private final Map<Long, Participant> participants = new ConcurrentHashMap<>();

    public GameRank(List<GamePlayer> players) {
        players.forEach(this::addPlayer);
    }

    static GameRank restore(List<Participant> participants) {
        GameRank rank = new GameRank(List.of());
        participants.forEach(participant -> rank.participants.put(participant.player().memberId(), participant));
        return rank;
    }

    List<Participant> snapshot() {
        return List.copyOf(participants.values());
    }

    public void updatePoint(Long memberId) {
        participants.computeIfPresent(memberId, (id, participant) -> participant.scored());
    }

    public List<PlayerScore> getPlayerScores() {
        List<Participant> playing = participants.values().stream()
                .filter(Participant::playing)
                .toList();

        return playing.stream()
                .map(participant -> participant.toPlayerScore(rankAmong(playing, participant)))
                .sorted(Comparator.comparingInt(PlayerScore::rank)
                        .thenComparing(PlayerScore::memberId))
                .toList();
    }

    public GameOutcome scoredOutcome() {
        List<PlayerScore> finisherScores = getPlayerScores();
        int leaverRank = finisherScores.size() + 1;

        Stream<PlayerOutcome> finished = finisherScores.stream().map(PlayerOutcome::finished);
        Stream<PlayerOutcome> left = leavers().map(leaver -> PlayerOutcome.leftScored(leaver, leaverRank));
        return new GameOutcome(Stream.concat(finished, left).toList());
    }

    public GameOutcome cooperativeOutcome(boolean succeeded) {
        Stream<PlayerOutcome> finished = finishers().map(finisher -> PlayerOutcome.cooperated(finisher, succeeded));
        Stream<PlayerOutcome> left = leavers().map(PlayerOutcome::leftCooperation);
        return new GameOutcome(Stream.concat(finished, left).toList());
    }

    private Stream<Participant> finishers() {
        return participants.values().stream()
                .filter(Participant::playing)
                .sorted(Comparator.comparing(Participant::memberId));
    }

    private Stream<Participant> leavers() {
        return participants.values().stream()
                .filter(participant -> !participant.playing())
                .sorted(Comparator.comparing(Participant::memberId));
    }

    private static int rankAmong(List<Participant> playing, Participant participant) {
        long higherScorers = playing.stream()
                .filter(other -> other.score() > participant.score())
                .count();
        return (int) higherScorers + 1;
    }

    public void addPlayer(GamePlayer player) {
        participants.put(player.memberId(), Participant.joining(player));
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
