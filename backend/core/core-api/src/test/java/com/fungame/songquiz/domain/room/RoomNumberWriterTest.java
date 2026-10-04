package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.support.ApiIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RoomNumberWriterTest extends ApiIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 30;

    @Autowired
    private RoomNumberWriter roomNumberWriter;

    @Test
    @DisplayName("번호를 부를 때마다 올라간다.")
    void issues_increasing_numbers() {
        Long first = roomNumberWriter.issueNext();
        Long second = roomNumberWriter.issueNext();

        assertThat(second).isGreaterThan(first);
    }

    @Test
    @DisplayName("동시에 발급해도 같은 번호가 두 번 나가지 않는다.")
    void never_issues_the_same_number_twice() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

        try {
            List<Callable<Long>> calls = IntStream.range(0, CONCURRENT_REQUESTS)
                    .<Callable<Long>>mapToObj(i -> roomNumberWriter::issueNext)
                    .toList();

            List<Long> issued = pool.invokeAll(calls).stream()
                    .map(RoomNumberWriterTest::valueOf)
                    .toList();

            assertThat(Set.copyOf(issued)).hasSize(CONCURRENT_REQUESTS);
        } finally {
            pool.shutdownNow();
        }
    }

    private static Long valueOf(Future<Long> issued) {
        try {
            return issued.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
