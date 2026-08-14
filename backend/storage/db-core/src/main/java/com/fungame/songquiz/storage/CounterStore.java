package com.fungame.songquiz.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CounterStore {

    private final CounterRepository counterRepository;

    public CounterEntity findByName(String name) {
        return counterRepository.findByName(name);
    }
}
