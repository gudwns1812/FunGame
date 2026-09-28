package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 방 번호를 발급한다. 메모리에 두면 재기동할 때마다 1번부터 다시 나온다. */
@Component
@RequiredArgsConstructor
public class RoomNumberWriter {

    private static final String GAME_ROOM_COUNTER = "GAME_ROOM_COUNTER";

    private final CounterRepository counterRepository;

    @Transactional
    public Long issueNext() {
        if (counterRepository.increment(GAME_ROOM_COUNTER) == 0) {
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        }

        CounterEntity counter = counterRepository.findByName(GAME_ROOM_COUNTER);

        return counter.getCount();
    }
}
