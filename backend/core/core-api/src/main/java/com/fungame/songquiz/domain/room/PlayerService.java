package com.fungame.songquiz.domain.room;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerNumberWriter playerNumberWriter;

    public Long getAutoKey() {
        return playerNumberWriter.issueNext();
    }
}
