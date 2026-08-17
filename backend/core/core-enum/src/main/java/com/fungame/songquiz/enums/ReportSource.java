package com.fungame.songquiz.enums;

public enum ReportSource {
    IN_GAME, LOBBY;

    public boolean needsRoom() {
        return this == IN_GAME;
    }
}
