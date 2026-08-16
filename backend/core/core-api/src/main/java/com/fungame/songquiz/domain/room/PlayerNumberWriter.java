package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlayerNumberWriter {

    private static final String PLAYER_COUNTER = "PLAYER_COUNTER";

    private final CounterRepository counterRepository;

    @Transactional
    public Long issueNext() {
        CounterEntity counter = counterRepository.findByName(PLAYER_COUNTER);
        counter.increment();

        return counter.getCount();
    }
}
