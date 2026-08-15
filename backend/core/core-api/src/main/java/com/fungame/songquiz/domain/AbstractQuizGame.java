package com.fungame.songquiz.domain;

import com.fungame.songquiz.enums.ActionResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractQuizGame implements Game {
    protected final AtomicBoolean isRoundProcessing = new AtomicBoolean(false);
    protected final Map<Long, Boolean> skipVotes = new ConcurrentHashMap<>();
    protected List<GamePlayer> players;

    protected AbstractQuizGame(List<GamePlayer> players) {
        if (players != null) {
            this.players = new ArrayList<>(players);
            initSkipVotes();
        }
    }

    @Override
    public void setPlayers(List<GamePlayer> players) {
        this.players = new ArrayList<>(players);
        initSkipVotes();
    }

    @Override
    public void removePlayer(Long memberId) {
        if (players == null) {
            return;
        }

        players.removeIf(player -> player.memberId().equals(memberId));
        skipVotes.remove(memberId);
    }

    @Override
    public void restorePlayer(GamePlayer player) {
        if (players == null || hasPlayer(player.memberId())) {
            return;
        }

        players.add(player);
        skipVotes.put(player.memberId(), false);
    }

    protected boolean hasPlayer(Long memberId) {
        return players.stream().anyMatch(player -> player.memberId().equals(memberId));
    }

    private void initSkipVotes() {
        skipVotes.clear();
        players.forEach(player -> skipVotes.put(player.memberId(), false));
    }

    @Override
    public ActionResult handleAction(GameAction action) {
        return switch (action.type()) {
            case SUBMIT_ANSWER -> processAnswer(action.memberId(), action.value());
            case SKIP_VOTE -> processSkipVote(action.memberId());
            default -> ActionResult.NO_ACTION;
        };
    }

    protected abstract ActionResult processAnswer(Long memberId, String answer);

    private ActionResult processSkipVote(Long memberId) {
        if (!hasPlayer(memberId)) {
            return ActionResult.NO_ACTION;
        }
        skipVotes.put(memberId, true);
        return isSkipThresholdReached() ? ActionResult.SKIP_VOTE_SUCCESS : ActionResult.ACTION_SUCCESS;
    }

    private boolean isSkipThresholdReached() {
        long count = skipVotes.values().stream().filter(v -> v).count();
        int required = Math.max(1, players.size() - 1);
        return count >= required;
    }

    public boolean startProcessing() {
        return isRoundProcessing.compareAndSet(false, true);
    }

    public void resetRoundState() {
        isRoundProcessing.set(false);
        skipVotes.replaceAll((k, v) -> false);
    }
}
