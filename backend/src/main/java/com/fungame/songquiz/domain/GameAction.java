package com.fungame.songquiz.domain;

public record GameAction(String playerName, ActionType type, String value) {
    public static GameAction submitAnswer(String playerName, String answer) {
        return new GameAction(playerName, ActionType.SUBMIT_ANSWER, answer);
    }

    public static GameAction skipVote(String playerName) {
        return new GameAction(playerName, ActionType.SKIP_VOTE, null);
    }
}
