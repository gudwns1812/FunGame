package com.fungame.songquiz.domain;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameTimer {

    private final ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(10);

    
}
