package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.domain.dto.HangmanStatusDto;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class HangmanGame extends AbstractQuizGame {
    private final String answer;
    private String currentDisplay;
    private final Set<Character> wrongLetters;
    private int remainingTries;
    private int currentTurnIndex;
    private List<String> playerOrder;

    private static final int DEFAULT_TRIES = 6;

    private HangmanGame(String answer) {
        super(List.of());
        this.answer = answer.toUpperCase();
        this.playerOrder = new ArrayList<>();
        this.wrongLetters = new LinkedHashSet<>();
        this.remainingTries = DEFAULT_TRIES;
        this.currentTurnIndex = 0;
        this.currentDisplay = initializeDisplay(this.answer);
    }

    public static HangmanGame create(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new CoreException(ErrorType.HANGMAN_ANSWER_EMPTY);
        }
        return new HangmanGame(answer);
    }

    public void initPlayers(List<String> players) {
        if (players == null || players.isEmpty()) {
            throw new CoreException(ErrorType.HANGMAN_PLAYER_EMPTY);
        }
        this.playerOrder = new ArrayList<>(players);
    }

    private String initializeDisplay(String answer) {
        return answer.chars()
                .mapToObj(c -> (char) c == ' ' ? " " : "_")
                .collect(Collectors.joining(" "));
    }

    public ActionResult guess(String playerId, char letter) {
        letter = Character.toUpperCase(letter);
        validateGuess(playerId, letter);

        boolean isCorrect = false;
        if (answer.indexOf(letter) >= 0) {
            updateDisplay(letter);
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

    private void validateGuess(String playerId, char letter) {
        if (!playerOrder.get(currentTurnIndex).equals(playerId)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE); // "당신의 차례가 아닙니다." 의미로 사용
        }
        if (remainingTries <= 0 || isGameWon()) {
            throw new CoreException(ErrorType.DEFAULT_ERROR); // "이미 종료된 게임입니다."
        }
        if (wrongLetters.contains(letter) || currentDisplay.indexOf(letter) >= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE); // "이미 선택한 글자입니다."
        }
    }

    private void updateDisplay(char letter) {
        StringBuilder nextDisplay = new StringBuilder();
        String cleanDisplay = currentDisplay.replace(" ", "");
        for (int i = 0; i < answer.length(); i++) {
            if (answer.charAt(i) == letter) {
                nextDisplay.append(letter);
            } else {
                nextDisplay.append(cleanDisplay.charAt(i));
            }
            if (i < answer.length() - 1) {
                nextDisplay.append(" ");
            }
        }
        this.currentDisplay = nextDisplay.toString();
    }

    private void moveToNextTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % playerOrder.size();
    }

    public boolean isGameWon() {
        return !currentDisplay.contains("_");
    }

    public String getCurrentTurnPlayer() {
        if (playerOrder == null || playerOrder.isEmpty()) {
            throw new CoreException(ErrorType.HANGMAN_PLAYER_EMPTY);
        }

        return playerOrder.get(currentTurnIndex % playerOrder.size());
    }

    @Override
    protected ActionResult processAnswer(String playerName, String answer) {
        // 단어 전체 정답 제출 시 처리
        if (this.answer.equalsIgnoreCase(answer.trim())) {
            this.currentDisplay = this.answer.chars()
                    .mapToObj(c -> String.valueOf((char) c))
                    .collect(Collectors.joining(" "));
            return ActionResult.CORRECT;
        }
        return ActionResult.WRONG;
    }

    @Override
    public GameType getType() {
        return GameType.HANGMAN;
    }

    @Override
    public GameContentDto getStatus() {
        return GameContentDto.from(this,
                currentDisplay,
                wrongLetters.stream().map(String::valueOf).collect(Collectors.joining(",")),
                String.valueOf(remainingTries),
                getCurrentTurnPlayer(),
                String.valueOf(remainingTries <= 0 || isGameWon()),
                String.valueOf(isGameWon())
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
    public void nextRound() {
        // 단판 게임이므로 별도 구현 없음
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
