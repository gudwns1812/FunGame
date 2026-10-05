package com.fungame.songquiz.support;

import com.fungame.songquiz.api.websocket.LobbyNotifier;
import com.fungame.songquiz.api.websocket.RedisStreamSpreader;
import com.fungame.songquiz.api.websocket.RoomLeaveGrace;
import com.fungame.songquiz.api.websocket.StompSessions;
import com.fungame.songquiz.domain.invite.RoomInviteService;
import com.fungame.songquiz.domain.member.DailyActiveMembers;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.quiz.QuizFactories;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.session.GameServiceRouter;
import com.fungame.songquiz.domain.session.GameSessionManager;
import com.fungame.songquiz.domain.session.GameTimer;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * 테스트 사이에 남는 공유 상태를 치운다.
 *
 * <p>통합 테스트가 애플리케이션을 한 번만 띄우므로 방·세션·접속 추적·초대 같은 메모리 상태가
 * 다음 테스트로 넘어간다. DB 만 비우면 절반만 치우는 것이다. 한 테스트가 만든 방이 다음 테스트의 "방 목록" 단언에 섞여 드는 식으로, 테스트가 늘수록 더 자주 터진다.
 *
 * <p>치울 빈은 {@link #STATEFUL_BEANS} 에 타입으로 적는다. 어느 필드를 비울지는 적지 않고
 * 리플렉션으로 찾는다 — 필드 이름이 바뀌어도 따라간다. 목록이 빠지는 것은 {@code SharedStateCoverageTest} 가 막는다.
 */
public class SharedStateCleaner {

    private static final List<Class<?>> STATEFUL_BEANS = List.of(
            GameRoomManager.class,
            GameSessionManager.class,
            GameTimer.class,
            MemberConnectionTracker.class,
            RoomInviteService.class,
            RoomLeaveGrace.class,
            StompSessions.class,
            DailyActiveMembers.class,
            LobbyNotifier.class,
            RedisStreamSpreader.class);

    /**
     * 기동 때 한 번 채우고 그 뒤로 바뀌지 않는 레지스트리. 테스트 사이에 비우면 앱이 죽는다. 상태를 든 것처럼 보이지만 치우면 안 되는 것들이라 여기 적어 구분한다.
     */
    private static final List<Class<?>> IMMUTABLE_REGISTRIES = List.of(
            QuizFactories.class,
            GameServiceRouter.class);

    private final ApplicationContext context;

    public SharedStateCleaner(ApplicationContext context) {
        this.context = context;
    }

    public void clean() {
        STATEFUL_BEANS.forEach(this::resetBean);
        clearCaches();
    }

    /**
     * 치우거나, 치우지 않기로 못 박은 타입. 둘 중 어디에도 없으면 분류를 안 한 것이다.
     */
    public static List<Class<?>> classifiedTypes() {
        return Stream.concat(STATEFUL_BEANS.stream(), IMMUTABLE_REGISTRIES.stream()).toList();
    }

    private void resetBean(Class<?> type) {
        Object bean = context.getBeanProvider(type).getIfAvailable();
        if (bean == null) {
            return;
        }

        ReflectionUtils.doWithFields(targetTypeOf(bean), field -> {
            if (Modifier.isStatic(field.getModifiers())) {
                return;
            }
            ReflectionUtils.makeAccessible(field);
            reset(ReflectionUtils.getField(field, bean));
        });
    }

    /**
     * 스파이로 감싼 빈은 프록시 클래스라 필드가 원본 클래스에 있다.
     */
    private Class<?> targetTypeOf(Object bean) {
        Class<?> type = bean.getClass();
        return type.getName().contains("$MockitoMock$") ? type.getSuperclass() : type;
    }

    private void reset(Object value) {
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(SharedStateCleaner::cancelIfScheduled);
            map.clear();
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(SharedStateCleaner::cancelIfScheduled);
            collection.clear();
            return;
        }
        if (value instanceof AtomicBoolean flag) {
            flag.set(false);
        }
    }

    /**
     * 예약된 작업을 버리기만 하면 나중에 깨어나 다음 테스트의 상태를 건드린다.
     */
    private static void cancelIfScheduled(Object value) {
        if (value instanceof Future<?> future) {
            future.cancel(false);
        }
    }

    private void clearCaches() {
        CacheManager cacheManager = context.getBeanProvider(CacheManager.class).getIfAvailable();
        if (cacheManager == null) {
            return;
        }

        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
