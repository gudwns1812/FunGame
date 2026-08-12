package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.RoomSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoomSettingsRequest {
    private GameType gameType;
    private int maxPlayers;
    private Category category;
    private int totalRound;
    private int difficulty;

    public RoomSettings applyTo(RoomSettings current) {
        return current.changeTo(gameType, maxPlayers, category, totalRound, difficulty);
    }
}
