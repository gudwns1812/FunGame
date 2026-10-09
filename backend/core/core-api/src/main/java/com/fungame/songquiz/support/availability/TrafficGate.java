package com.fungame.songquiz.support.availability;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrafficGate {

    private static final String NO_STATE_FILE = "";

    private final AtomicBoolean accepting;
    private final TrafficStateFile stateFile;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public TrafficGate(@Value("${app.traffic.accept-on-startup:true}") boolean acceptOnStartup,
                       @Value("${app.traffic.state-file:}") String stateFile,
                       ApplicationEventPublisher applicationEventPublisher) {
        this.stateFile = TrafficStateFile.at(stateFile);
        this.accepting = new AtomicBoolean(this.stateFile.read().orElse(acceptOnStartup));
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public TrafficGate(boolean acceptOnStartup, ApplicationEventPublisher applicationEventPublisher) {
        this(acceptOnStartup, NO_STATE_FILE, applicationEventPublisher);
    }

    @EventListener
    public void keepReadinessInStepWithTheGate(AvailabilityChangeEvent<ReadinessState> event) {
        if (event.getState() == ReadinessState.ACCEPTING_TRAFFIC && !accepting.get()) {
            announce(ReadinessState.REFUSING_TRAFFIC);
        }
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
        stateFile.write(nowAccepting);

        log.info("트래픽을 {} 한다. 전역 작업도 함께 {}", nowAccepting ? "받기 시작" : "받지 않기로",
                nowAccepting ? "돈다" : "멈춘다");
        announce(state);
    }

    private void announce(ReadinessState state) {
        AvailabilityChangeEvent.publish(applicationEventPublisher, this, state);
    }
}
