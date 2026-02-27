package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SongReader {
    private final SongRepository songRepository;

    public List<Long> findSongByCategoryWithCount(Category category, int count) {
        List<Long> songs = songRepository.findByCategory(category);

        Collections.shuffle(songs);

        return songs.stream()
                .limit(count)
                .collect(Collectors.toList());
    }
}
