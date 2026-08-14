package com.fungame.songquiz.controller.api;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.dto.OnlineMemberInfo;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.member.PlayerStatus;
import com.fungame.songquiz.support.MemberFixture;
import com.fungame.songquiz.support.MutableClock;
import com.fungame.songquiz.support.sse.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@DisplayName("SSE 로 실어 보내는 목록은 JSON 으로 전달된다")
class SsePayloadSerializationTest {

    private static final Long SUBSCRIBER_ID = 1L;

    private final MemberConnectionTracker memberConnectionTracker = new MemberConnectionTracker(
            event -> {
            },
            new MutableClock(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("UTC")));
    private final SseService sseService = new SseService(memberConnectionTracker);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new SseController(sseService))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(MemberAdapter.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new MemberAdapter(MemberFixture.withId(SUBSCRIBER_ID, "구독자"));
                    }
                })
                .build();
    }

    @Test
    @DisplayName("방 목록은 구독자에게 JSON 으로 나간다.")
    void roomsAreSentAsJson() throws Exception {
        MvcResult subscription = subscribe();

        sseService.broadcast("room-update", List.of(room()));

        assertThat(streamOf(subscription))
                .contains("event:room-update")
                .contains("\"title\":\"방 제목\"")
                .contains("\"currentPlayers\":2");
    }

    @Test
    @DisplayName("접속자 목록은 받는 사람에 맞춰 JSON 으로 나간다.")
    void onlineMembersAreSentAsJson() throws Exception {
        MvcResult subscription = subscribe();

        sseService.broadcastEach("presence-update", memberId -> List.of(onlineMember(memberId + 1)));

        assertThat(streamOf(subscription))
                .contains("event:presence-update")
                .contains("\"memberId\":" + (SUBSCRIBER_ID + 1))
                .contains("\"status\":\"LOBBY\"");
    }

    private MvcResult subscribe() throws Exception {
        return mockMvc.perform(get("/api/sse/subscribe").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    private static String streamOf(MvcResult subscription) throws Exception {
        return subscription.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static RoomInfo room() {
        return new RoomInfo(9L, "방 제목", 3L, "방장", GameRoomStatus.WAITING, 8, 2, GameType.SONG, CSQuizDifficulty.HARD);
    }

    private static OnlineMemberInfo onlineMember(Long memberId) {
        return new OnlineMemberInfo(memberId, "회원" + memberId, PlayerStatus.LOBBY, null);
    }
}
