package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateRoomRequest {
    private String title;
    private int maxPlayers;
    private String hostName;
    private Category category;
    private int songCount;
}
