package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.PlayerEntity;
import com.fungame.songquiz.storage.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository repository;

    public Long getAutoKey() {
        PlayerEntity save = repository.save(new PlayerEntity());
        return save.getKey();
    }
}
