package com.fungame.songquiz.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.support.StompMessages;
import com.fungame.songquiz.support.logging.LogContext;
import jakarta.servlet.FilterChain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class LogContextFilterTest {

    private static final Long MEMBER_ID = 17L;

    private final LogContextFilter filter = new LogContextFilter();
    private final List<Map<String, String>> seen = new ArrayList<>();
    private final FilterChain recordingChain = (request, response) -> seen.add(contextNow());

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("요청을 처리하는 동안 요청 id 와 로그인한 회원 id 가 보인다.")
    void a_signed_in_request_carries_request_and_member() throws Exception {
        signIn((Authentication) StompMessages.loggedIn(MEMBER_ID));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), recordingChain);

        assertThat(seen).singleElement().satisfies(context -> {
            assertThat(context.get(LogContext.REQUEST_ID)).hasSize(8);
            assertThat(context).containsEntry(LogContext.MEMBER_ID, String.valueOf(MEMBER_ID));
        });
    }

    @Test
    @DisplayName("로그인하지 않은 요청에는 요청 id 만 붙는다.")
    void an_anonymous_request_carries_only_the_request() throws Exception {
        signIn(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), recordingChain);

        assertThat(seen).singleElement().satisfies(context -> {
            assertThat(context).containsKey(LogContext.REQUEST_ID);
            assertThat(context).doesNotContainKey(LogContext.MEMBER_ID);
        });
    }

    @Test
    @DisplayName("요청마다 다른 id 를 받고, 요청이 끝나면 문맥을 지운다.")
    void each_request_gets_its_own_id_and_leaves_nothing_behind() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), recordingChain);
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), recordingChain);

        assertThat(seen).extracting(context -> context.get(LogContext.REQUEST_ID)).doesNotHaveDuplicates();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private static void signIn(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Map<String, String> contextNow() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return context == null ? Map.of() : new HashMap<>(context);
    }
}
