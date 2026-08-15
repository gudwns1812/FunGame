package com.fungame.songquiz.domain.gamecreator;

import com.fungame.songquiz.enums.Category;

public record SongGameCreateInfo(Category category, int songCount) implements GameCreateInfo {
}
