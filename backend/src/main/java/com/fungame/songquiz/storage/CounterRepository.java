package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepository extends JpaRepository<CounterEntity, Long> {
    CounterEntity findByName(String name);
}
