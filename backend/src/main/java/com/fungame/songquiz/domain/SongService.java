package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;

    public List<Long> getRandomSongIds(int count) {
        List<Long> allIds = songRepository.findAll().stream()
                .map(SongEntity::getId)
                .collect(Collectors.toList());

        Collections.shuffle(allIds);

        return allIds.stream()
                .limit(count)
                .collect(Collectors.toList());
    }
}
