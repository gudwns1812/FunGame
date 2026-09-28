package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 번호를 발급한다.
 * <p>
 * 메모리 카운터로 두면 재기동할 때마다 1번부터 다시 나와, 옛 번호를 들고 있던 클라이언트가
 * 전혀 다른 방에 들어간다. 인스턴스가 둘이면 둘이 나란히 1번을 발급한다. 그래서 DB 에 둔다.
 */
@Component
@RequiredArgsConstructor
public class RoomNumberWriter {

    private static final String GAME_ROOM_COUNTER = "GAME_ROOM_COUNTER";

    private final CounterRepository counterRepository;

    @Transactional
    public Long issueNext() {
        if (counterRepository.increment(GAME_ROOM_COUNTER) == 0) {
            // V21 이 모든 환경에 넣어 두므로 여기 오면 마이그레이션이 빠진 것이다.
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        }

        CounterEntity counter = counterRepository.findByName(GAME_ROOM_COUNTER);

        return counter.getCount();
    }
}
