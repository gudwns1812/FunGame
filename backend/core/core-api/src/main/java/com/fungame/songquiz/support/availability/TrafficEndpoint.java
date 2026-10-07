package com.fungame.songquiz.support.availability;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "traffic")
@RequiredArgsConstructor
public class TrafficEndpoint {

    private final TrafficGate trafficGate;

    @ReadOperation
    public Map<String, Object> read() {
        return Map.of("accepting", trafficGate.isAccepting());
    }

    @WriteOperation
    public Map<String, Object> write(boolean accepting) {
        if (accepting) {
            trafficGate.accept();
        } else {
            trafficGate.refuse();
        }

        return read();
    }
}
