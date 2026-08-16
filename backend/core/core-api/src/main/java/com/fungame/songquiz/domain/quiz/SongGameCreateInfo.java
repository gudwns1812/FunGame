package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;

public record SongGameCreateInfo(Category category, int songCount) implements GameCreateInfo {
}
