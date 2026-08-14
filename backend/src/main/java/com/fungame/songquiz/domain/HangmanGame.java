package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ActionResult;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class HangmanGame extends AbstractQuizGame {
    private static final int DEFAULT_TRIES = 6;
    private static final char BLANK = ' ';
    private static final String HIDDEN_LETTER = "_";
    private static final String LETTER_DELIMITER = " ";

    private final String answer;
    private final Set<Character> correctLetters;
    private final Set<Character> wrongLetters;
    private int remainingTries;
    private int currentTurnIndex;
    private List<GamePlayer> playerOrder;

    private HangmanGame(String answer) {
        super(List.of());
        this.answer = answer.toUpperCase();
        this.playerOrder = new ArrayList<>();
        this.correctLetters = new LinkedHashSet<>();
        this.wrongLetters = new LinkedHashSet<>();
        this.remainingTries = DEFAULT_TRIES;
        this.currentTurnIndex = 0;
    }

    public static HangmanGame create(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new CoreException(ErrorType.HANGMAN_ANSWER_EMPTY);
        }
        return new HangmanGame(answer);
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
    public void removePlayer(Long memberId) {
        super.removePlayer(memberId);

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
    public void restorePlayer(GamePlayer player) {
        super.restorePlayer(player);

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
    protected ActionResult processAnswer(Long memberId, String answer) {
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
    public GameContentDto getStatus() {
        GamePlayer currentTurnPlayer = getCurrentTurnPlayer();

        return GameContentDto.from(this,
                getCurrentDisplay(),
                wrongLetters.stream().map(String::valueOf).collect(Collectors.joining(",")),
                String.valueOf(remainingTries),
                currentTurnPlayer.nickname(),
                String.valueOf(remainingTries <= 0 || isGameWon()),
                String.valueOf(isGameWon()),
                String.valueOf(currentTurnPlayer.memberId())
        );
    }

    @Override
    public GameInfo getGameInfo() {
        return new GameInfo(getType().name(), "Hangman", DEFAULT_TRIES);
    }

    @Override
    public GameAnswerDto getAnswer() {
        return new GameAnswerDto(this, List.of(answer));
    }

    @Override
    public void startRound() {
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
