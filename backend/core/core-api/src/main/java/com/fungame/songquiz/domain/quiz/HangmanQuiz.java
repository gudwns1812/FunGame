package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class HangmanQuiz extends AbstractQuiz {
    private static final int DEFAULT_TRIES = 6;
    private static final char BLANK = ' ';
    private static final String HIDDEN_LETTER = "_";
    private static final String LETTER_DELIMITER = " ";

    private final Long wordId;
    private final String answer;
    private final Set<Character> correctLetters;
    private final Set<Character> wrongLetters;
    private int remainingTries;
    private int currentTurnIndex;
    private List<GamePlayer> playerOrder;

    private HangmanQuiz(Long wordId, String answer) {
        this.wordId = wordId;
        this.answer = answer.toUpperCase();
        this.playerOrder = new ArrayList<>();
        this.correctLetters = new LinkedHashSet<>();
        this.wrongLetters = new LinkedHashSet<>();
        this.remainingTries = DEFAULT_TRIES;
        this.currentTurnIndex = 0;
    }

    public static HangmanQuiz create(HangmanWord word) {
        if (word == null || word.value() == null || word.value().isBlank()) {
            throw new CoreException(ErrorType.HANGMAN_ANSWER_EMPTY);
        }
        return new HangmanQuiz(word.id(), word.value());
    }

    public void initPlayers(List<GamePlayer> players) {
        if (players == null || players.isEmpty()) {
            throw new CoreException(ErrorType.HANGMAN_PLAYER_EMPTY);
        }
        this.playerOrder = new ArrayList<>(players);
    }

    public String getCurrentDisplay() {
        return answer.chars()
                .mapToObj(codePoint -> letterDisplay((char) codePoint))
                .collect(Collectors.joining(LETTER_DELIMITER));
    }

    private String letterDisplay(char letter) {
        if (letter == BLANK) {
            return LETTER_DELIMITER;
        }
        return correctLetters.contains(letter) ? String.valueOf(letter) : HIDDEN_LETTER;
    }

    public ActionResult guess(Long memberId, char letter) {
        letter = Character.toUpperCase(letter);
        validateGuess(memberId, letter);

        boolean isCorrect = false;
        if (answer.indexOf(letter) >= 0) {
            correctLetters.add(letter);
            isCorrect = true;
        } else {
            wrongLetters.add(letter);
            remainingTries--;
        }

        moveToNextTurn();

        if (isCorrect) {
            return isGameWon() ? ActionResult.CORRECT : ActionResult.ACTION_SUCCESS;
        }
        return remainingTries <= 0 ? ActionResult.WRONG : ActionResult.ACTION_SUCCESS;
    }

    private void validateGuess(Long memberId, char letter) {
        if (!playerOrder.get(currentTurnIndex).memberId().equals(memberId)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE); // "당신의 차례가 아닙니다." 의미로 사용
        }
        if (remainingTries <= 0 || isGameWon()) {
            throw new CoreException(ErrorType.DEFAULT_ERROR); // "이미 종료된 게임입니다."
        }
        if (wrongLetters.contains(letter) || correctLetters.contains(letter)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE); // "이미 선택한 글자입니다."
        }
    }

    private void moveToNextTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % playerOrder.size();
    }

    @Override
    public void dropPlayer(Long memberId) {
        int leaverIndex = indexOf(memberId);
        if (leaverIndex < 0) {
            return;
        }

        playerOrder.remove(leaverIndex);

        if (playerOrder.isEmpty()) {
            currentTurnIndex = 0;
            return;
        }

        if (leaverIndex < currentTurnIndex) {
            currentTurnIndex--;
        }
        currentTurnIndex %= playerOrder.size();
    }

    private int indexOf(Long memberId) {
        for (int i = 0; i < playerOrder.size(); i++) {
            if (playerOrder.get(i).memberId().equals(memberId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void takeBackPlayer(GamePlayer player) {
        if (indexOf(player.memberId()) < 0) {
            playerOrder.add(player);
        }
    }

    public boolean isGameWon() {
        return answer.chars()
                .filter(codePoint -> (char) codePoint != BLANK)
                .allMatch(codePoint -> correctLetters.contains((char) codePoint));
    }

    public GamePlayer getCurrentTurnPlayer() {
        if (playerOrder == null || playerOrder.isEmpty()) {
            throw new CoreException(ErrorType.HANGMAN_PLAYER_EMPTY);
        }

        return playerOrder.get(currentTurnIndex % playerOrder.size());
    }

    @Override
    public ActionResult submitAnswer(Long memberId, String answer) {
        if (!this.answer.equalsIgnoreCase(answer.trim())) {
            return ActionResult.WRONG;
        }

        revealAllLetters();
        return ActionResult.CORRECT;
    }

    private void revealAllLetters() {
        answer.chars()
                .filter(codePoint -> (char) codePoint != BLANK)
                .forEach(codePoint -> correctLetters.add((char) codePoint));
    }

    @Override
    public GameType getType() {
        return GameType.HANGMAN;
    }

    @Override
    public QuizContent getStatus() {
        GamePlayer currentTurnPlayer = getCurrentTurnPlayer();
        String display = getCurrentDisplay();

        return new QuizContent(display, List.of(
                display,
                wrongLetters.stream().map(String::valueOf).collect(Collectors.joining(",")),
                String.valueOf(remainingTries),
                currentTurnPlayer.nickname(),
                String.valueOf(remainingTries <= 0 || isGameWon()),
                String.valueOf(isGameWon()),
                String.valueOf(currentTurnPlayer.memberId())
        ));
    }

    @Override
    public QuizInfo getQuizInfo() {
        return new QuizInfo(getType().name(), "Hangman", DEFAULT_TRIES);
    }

    @Override
    public Long getCurrentContentId() {
        return wordId;
    }

    @Override
    public QuizAnswer getAnswer() {
        return QuizAnswer.withoutExplanation(answer);
    }

    @Override
    public void startRound() {
    }

    @Override
    public boolean isRoundStarted() {
        return true;
    }

    @Override
    public boolean isLast() {
        return true;
    }

    @Override
    public int getCurrentRound() {
        return 1;
    }

    @Override
    public int getTotalRound() {
        return 1;
    }

    @Override
    public String getHint() {
        return "틀린 글자들: " + wrongLetters.toString();
    }
}
