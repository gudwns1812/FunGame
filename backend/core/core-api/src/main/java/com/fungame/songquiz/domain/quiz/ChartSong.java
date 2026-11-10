package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ChartSong(long melonId, SongTitle title, String singer, long likes) {

    private static final long POPULAR_LIKES = 50_000;
    private static final String SINGER_DELIMITER = ", ";
    private static final String STORED_ANSWER_DELIMITER = ",";
    private static final int MAX_STORED_ANSWERS_LENGTH = 255;
    private static final int NO_PLAY_OFFSET = 0;

    public static ChartSong of(long melonId, String rawTitle, List<String> singers, long likes) {
        String singer = singers.stream()
                .map(name -> name.replace(' ', ' ').strip())
                .collect(Collectors.joining(SINGER_DELIMITER));

        return new ChartSong(melonId, SongTitle.parse(rawTitle), singer, likes);
    }

    public boolean isPopular() {
        return likes >= POPULAR_LIKES;
    }

    public boolean needsReading() {
        return !Choseong.isHangulTitle(title.name());
    }

    public Song toSong(LocalDate releaseDate, List<Category> categories) {
        if (needsReading()) {
            throw new IllegalStateException("한글이 아닌 제목은 읽는 법이 있어야 곡으로 만들 수 있다: " + title.name());
        }

        List<String> candidates = Stream.of(List.of(title.name()), title.aliases())
                .flatMap(List::stream)
                .toList();

        return song(releaseDate, categories, Choseong.of(title.name()), candidates);
    }

    public Song toSong(LocalDate releaseDate, List<Category> categories, TitleReading reading) {
        if (!reading.isUsable()) {
            throw new IllegalArgumentException("한글 독음이 아니다: " + reading.pronunciation());
        }

        List<String> candidates = Stream.of(
                        List.of(title.name(), reading.pronunciation()), title.aliases(), reading.aliases())
                .flatMap(List::stream)
                .toList();

        return song(releaseDate, categories, Choseong.of(reading.pronunciation()), candidates);
    }

    private Song song(LocalDate releaseDate, List<Category> categories, String hint, List<String> candidates) {
        return Song.of(title.name(), singer, categories, releaseDate, null, NO_PLAY_OFFSET,
                storableAnswers(candidates), hint);
    }

    private List<String> storableAnswers(List<String> candidates) {
        List<String> answers = new ArrayList<>();
        int storedLength = title.name().length();

        for (String candidate : candidates) {
            String answer = typedForm(candidate);
            boolean redundant = answer.isEmpty() || answer.equals(title.name()) || answers.contains(answer);
            int lengthWithAnswer = storedLength + STORED_ANSWER_DELIMITER.length() + answer.length();
            if (redundant || lengthWithAnswer > MAX_STORED_ANSWERS_LENGTH) {
                continue;
            }

            answers.add(answer);
            storedLength = lengthWithAnswer;
        }

        return answers;
    }

    private static String typedForm(String text) {
        StringBuilder typed = new StringBuilder();
        text.codePoints()
                .filter(Character::isLetterOrDigit)
                .map(Character::toLowerCase)
                .forEach(typed::appendCodePoint);

        return typed.toString();
    }
}
