package com.fungame.songquiz.support;

import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 도메인 이벤트를 기록해 두는 테스트용 리스너.
 *
 * <p>한 테스트만 쓰더라도 공통 바탕에 둔다. 특정 클래스에서만 @Import 하면 그 클래스만
 * 컨텍스트가 갈려 애플리케이션을 통째로 다시 띄운다. 기록만 하는 리스너라 어디 있어도 해롭지 않다.
 */
public class TestEventCapture {

    private final List<Object> events = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    public void capture(Object event) {
        if (event.getClass().getPackageName().startsWith("com.fungame.songquiz.domain")) {
            events.add(event);
        }
    }

    public void clear() {
        events.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getEvents(Class<T> type) {
        synchronized (events) {
            return events.stream()
                    .filter(type::isInstance)
                    .map(e -> (T) e)
                    .toList();
        }
    }
}
