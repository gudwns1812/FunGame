package com.fungame.songquiz.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractQuizGame implements Game {
    protected final AtomicBoolean isRoundProcessing = new AtomicBoolean(false);
    protected final Map<String, Boolean> skipVotes = new ConcurrentHashMap<>();
    protected List<String> players;

    protected AbstractQuizGame(List<String> players) {
        if (players != null) {
            this.players = new ArrayList<>(players);
            initSkipVotes();
        }
    }

    @Override
    public void setPlayers(List<String> players) {
        this.players = new ArrayList<>(players);
        initSkipVotes();
    }

    @Override
    public void removePlayer(String playerName) {
        if (players == null) {
            return;
        }

        players.remove(playerName);
        skipVotes.remove(playerName);
    }

    @Override
    public void restorePlayer(String playerName) {
        if (players == null || players.contains(playerName)) {
            return;
        }

        players.add(playerName);
        skipVotes.put(playerName, false);
    }

    private void initSkipVotes() {
        skipVotes.clear();
        players.forEach(player -> skipVotes.put(player, false));
    }

    @Override
    public ActionResult handleAction(GameAction action) {
        return switch (action.type()) {
            case SUBMIT_ANSWER -> processAnswer(action.playerName(), action.value());
            case SKIP_VOTE -> processSkipVote(action.playerName());
            default -> ActionResult.NO_ACTION;
        };
    }

    protected abstract ActionResult processAnswer(String playerName, String answer);

    private ActionResult processSkipVote(String playerName) {
        if (!players.contains(playerName)) {
            return ActionResult.NO_ACTION;
        }
        skipVotes.put(playerName, true);
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
