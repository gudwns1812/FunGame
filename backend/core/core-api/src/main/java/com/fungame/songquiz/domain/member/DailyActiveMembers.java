package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberActivityDao;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class DailyActiveMembers {

    static final String FIRST_SEEN_METER = "fungame.member.first.seen";

    private final MemberActivityDao memberActivityDao;
    private final Clock clock;
    private final Counter firstSeen;

    private final Set<Long> memberIdsSeenToday = new HashSet<>();
    private LocalDate memoDate;

    public DailyActiveMembers(MemberActivityDao memberActivityDao, Clock clock, MeterRegistry meterRegistry) {
        this.memberActivityDao = memberActivityDao;
        this.clock = clock;
        this.firstSeen = Counter.builder(FIRST_SEEN_METER)
                .description("그날 처음 접속한 회원 수. DAU 는 24시간 증가분으로 구한다")
                .register(meterRegistry);
        this.memoDate = today();
    }

    public void record(Long memberId) {
        LocalDate today = today();

        if (!isFirstVisitToday(memberId, today)) {
            return;
        }

        try {
            if (memberActivityDao.recordActive(memberId, today)) {
                firstSeen.increment();
            }
        } catch (Exception e) {
            // 지표를 남기려다 접속 처리를 깨뜨리지 않는다. 메모에서 빼 두어 다음 접속에 다시 시도한다.
            forget(memberId);
            log.warn("회원 {} 의 활동일 기록에 실패했다", memberId, e);
        }
    }

    private synchronized boolean isFirstVisitToday(Long memberId, LocalDate today) {
        if (!today.equals(memoDate)) {
            memberIdsSeenToday.clear();
            memoDate = today;
        }

        return memberIdsSeenToday.add(memberId);
    }

    private synchronized void forget(Long memberId) {
        memberIdsSeenToday.remove(memberId);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
