package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizFactoryRegistry;
import com.fungame.songquiz.domain.room.GameRoom;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.RoomStore;
import com.fungame.songquiz.domain.room.RoomTable;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSessionManager {

    private final QuizFactoryRegistry quizFactoryRegistry;
    private final RoomStore roomStore;

    public Quiz createQuiz(RoomSettings settings) {
        return quizFactoryRegistry.create(settings);
    }

    public Optional<GameSession> find(Long roomId) {
        return roomStore.findTable(roomId).flatMap(RoomTable::game);
    }

    public <T> Optional<T> update(Long roomId, Function<GameSession, T> change) {
        try {
            return roomStore.updateTable(roomId, table -> table.game().map(change));
        } catch (CoreException e) {
            if (e.getType() == ErrorType.GAME_ROOM_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public int count() {
        return (int) roomStore.findAll().stream()
                .filter(GameRoom::isPlaying)
                .count();
    }
}
