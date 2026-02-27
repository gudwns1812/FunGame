package com.fungame.songquiz.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateRoomRequest {
    private String title;
    private int maxPlayers;
    private String name;
}
