package com.fungame.songquiz.domain.quiz;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SongQuizSnapshot.class, name = "SONG"),
        @JsonSubTypes.Type(value = CsQuizSnapshot.class, name = "CS"),
        @JsonSubTypes.Type(value = HangmanQuizSnapshot.class, name = "HANGMAN")
})
public sealed interface QuizSnapshot permits SongQuizSnapshot, CsQuizSnapshot, HangmanQuizSnapshot {

    Quiz restore();
}
