package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private static final String PLAYER_COUNTER = "PLAYER_COUNTER";
    private final CounterRepository repository;

    @Transactional
    public Long getAutoKey() {
        CounterEntity counter = repository.findByName(PLAYER_COUNTER);
        counter.increment();
        return counter.getCount();
    }
}
