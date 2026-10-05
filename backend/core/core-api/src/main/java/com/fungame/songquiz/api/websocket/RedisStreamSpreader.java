package com.fungame.songquiz.api.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamSpreader {

    private static final String WORKER_THREAD_NAME = "broadcast-pump";

    private final BroadcastMessages messages;
    private final BroadcastQueue queue;
    private final BroadcastStream stream;

    private volatile boolean running;
    private Thread worker;

    public void spread(String destination, String user, Object payload) {
        messages.of(destination, user, payload).ifPresent(queue::offer);
    }

    void pumpOnce() throws InterruptedException {
        BroadcastMessage message = queue.pollFresh();

        if (message == null) {
            return;
        }

        try {
            stream.write(message);
        } catch (RuntimeException e) {
            if (running) {
                log.error("다른 인스턴스로 브로드캐스트를 전파하지 못했다: {}", message.destination(), e);
            }
        }
    }

    @PostConstruct
    void startWorker() {
        running = true;
        worker = new Thread(this::pump, WORKER_THREAD_NAME);
        worker.start();
    }

    private void pump() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                pumpOnce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @PreDestroy
    void stopWorker() {
        running = false;

        if (worker != null) {
            worker.interrupt();
        }
    }
}
