package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizFactories;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.RoomSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GameSessionManager {
    private final QuizFactories quizFactories;
    private final Map<Long, GameSession> manager = new ConcurrentHashMap<>();

    public Quiz createQuiz(RoomSettings settings) {
        return quizFactories.create(settings);
    }

    public GameSession startGame(Long roomId, RoomSettings settings, List<GamePlayer> players) {
        return startGame(roomId, createQuiz(settings), players);
    }

    public GameSession startGame(Long roomId, Quiz quiz, List<GamePlayer> players) {
        GameSession gameSession = new GameSession(quiz, players);
        manager.put(roomId, gameSession);

        return gameSession;
    }

    public GameSession getGameSession(Long roomId) {
        return manager.get(roomId);
    }

    public void endGameSession(Long roomId) {
        manager.remove(roomId);
    }
}
