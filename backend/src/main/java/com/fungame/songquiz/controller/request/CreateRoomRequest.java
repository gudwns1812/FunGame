package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.gamecreator.CsQuizGameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.GameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.SongGameCreateInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {
    private GameType gameType;
    private String title;
    private int maxPlayers;
    private String hostName;
    private Category category;
    private int totalRound;

    public GameCreateInfo toGameInfo() {
        return switch (gameType) {
            case SONG -> new SongGameCreateInfo(category, totalRound);
            case CS -> new CsQuizGameCreateInfo(totalRound);
            case NONE -> null;
        };
    }
}
