package com.fungame.songquiz.domain.room;

public record JoinResult(int playerNumber, boolean newlyJoined, RoomStateInfo state) {
}
