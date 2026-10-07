package com.fungame.songquiz.support.availability;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrafficGate {

    private final AtomicBoolean accepting;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TrafficGate(@Value("${app.traffic.accept-on-startup:true}") boolean acceptOnStartup,
                       ApplicationEventPublisher applicationEventPublisher) {
        this.accepting = new AtomicBoolean(acceptOnStartup);
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public boolean isAccepting() {
        return accepting.get();
    }

    public void accept() {
        changeTo(true, ReadinessState.ACCEPTING_TRAFFIC);
    }

    public void refuse() {
        changeTo(false, ReadinessState.REFUSING_TRAFFIC);
    }

    private void changeTo(boolean nowAccepting, ReadinessState state) {
        if (!accepting.compareAndSet(!nowAccepting, nowAccepting)) {
            return;
        }

        log.info("트래픽을 {} 한다. 전역 작업도 함께 {}", nowAccepting ? "받기 시작" : "받지 않기로",
                nowAccepting ? "돈다" : "멈춘다");
        AvailabilityChangeEvent.publish(applicationEventPublisher, this, state);
    }
}
