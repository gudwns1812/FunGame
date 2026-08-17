package com.fungame.songquiz.acceptance;

import com.fungame.songquiz.controller.websocket.StompDestination;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.domain.room.RoomStateInfo;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.storage.MySqlTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.awaitility.Awaitility.await;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainer.class)
@TestPropertySource(properties = {
        "spring.session.jdbc.initialize-schema=always",
        "app.song-scrape.enabled=false"
})
class RealtimeChannelAcceptanceTest {

    private static final String PASSWORD = "password1";
    private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration LONGER_THAN_LEAVE_GRACE = Duration.ofSeconds(25);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GameRoomService gameRoomService;

    private WebSocketStompClient stompClient;
    private Actor host;
    private Actor guest;

    @BeforeEach
    void setUp() {
        emptyEveryRoom();
        memberRepository.deleteAll();
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        host = signUpAndConnect("hostuser", "방장");
        guest = signUpAndConnect("guestuser", "참가자");
    }

    @AfterEach
    void tearDown() {
        host.close();
        guest.close();
        stompClient.stop();
        emptyEveryRoom();
        memberRepository.deleteAll();
    }

    private void emptyEveryRoom() {
        gameRoomService.findAllRooms().stream()
                .map(RoomInfo::roomId)
                .forEach(this::emptyRoom);
    }

    private void emptyRoom(Long roomId) {
        RoomStateInfo state = gameRoomService.findRoomState(roomId);
        state.players().forEach(player -> gameRoomService.leaveRoom(roomId, player.memberId()));
    }

    @Test
    @DisplayName("방 목록은 로비 토픽으로 밀려온다.")
    void roomListIsPushedToLobbyTopic() {
        host.subscribe(StompDestination.LOBBY);

        Long roomId = host.createRoom("방 하나");

        Map<String, Object> pushedRoom = host.awaitRoomInLobby(roomId, room -> hasPlayers(room, 1));
        assertThat(pushedRoom.get("title")).isEqualTo("방 하나");
    }

    @Test
    @DisplayName("접속자 목록은 받는 사람마다 자기 자신이 빠진 채로 온다.")
    void presenceExcludesTheViewer() {
        host.subscribe(userQueue(StompDestination.PRESENCE));
        guest.subscribe(userQueue(StompDestination.PRESENCE));

        host.createRoom("접속자 확인용 방");

        assertThat(nicknamesOf(host.takeList(userQueue(StompDestination.PRESENCE)))).containsExactly("참가자");
        assertThat(nicknamesOf(guest.takeList(userQueue(StompDestination.PRESENCE)))).containsExactly("방장");
    }

    @Test
    @DisplayName("방 소속이 아닌 회원의 구독은 등록되지 않는다.")
    void outsiderSubscriptionIsNotRegistered() {
        Long roomId = host.createRoom("소속 확인용 방");
        String roomTopic = StompDestination.room(roomId);
        host.subscribe(roomTopic);
        guest.subscribe(roomTopic);

        guest.join(roomId);

        assertThat(host.takeMap(roomTopic).get("type")).isEqualTo("PLAYER_JOIN");
        assertThat(guest.poll(roomTopic)).isNull();
    }

    @Test
    @DisplayName("소속을 확정한 뒤 다시 구독하면 그때부터 방 이벤트를 받는다.")
    void subscriptionWorksAfterJoin() {
        Long roomId = host.createRoom("재구독 확인용 방");
        String roomTopic = StompDestination.room(roomId);

        guest.join(roomId);
        guest.subscribe(roomTopic);
        guest.toggleReady(roomId);

        assertThat(guest.takeMap(roomTopic).get("type")).isEqualTo("PLAYER_READY");
    }

    @Test
    @DisplayName("방 이벤트는 바뀐 뒤의 방 전체와 올라가는 version 을 싣고 온다.")
    void roomEventCarriesWholeRoomWithRisingVersion() {
        Long roomId = host.createRoom("전체 상태 확인용 방");
        String roomTopic = StompDestination.room(roomId);
        host.subscribe(roomTopic);

        guest.join(roomId);
        Map<String, Object> afterJoin = roomStateOf(host.takeMap(roomTopic));

        guest.toggleReady(roomId);
        Map<String, Object> afterReady = roomStateOf(host.takeMap(roomTopic));

        assertThat(nicknamesOf(playersOf(afterJoin))).containsExactly("방장", "참가자");
        assertThat(versionOf(afterReady)).isGreaterThan(versionOf(afterJoin));
        assertThat(host.readRoomState(roomId).get("version")).isEqualTo(afterReady.get("version"));
    }

    @Test
    @DisplayName("퇴장은 유예 없이 곧바로 방 목록과 방 이벤트에 반영된다.")
    void leaveIsAppliedWithoutGrace() {
        Long roomId = host.createRoom("퇴장 확인용 방");
        String roomTopic = StompDestination.room(roomId);
        guest.join(roomId);
        host.subscribe(roomTopic);
        host.subscribe(StompDestination.LOBBY);

        guest.leave(roomId);

        Map<String, Object> leaveEvent = host.takeMap(roomTopic);
        assertThat(leaveEvent.get("type")).isEqualTo("PLAYER_LEAVE");
        assertThat(nicknamesOf(playersOf(roomStateOf(leaveEvent)))).containsExactly("방장");
        assertThat(host.awaitRoomInLobby(roomId, room -> hasPlayers(room, 1))).isNotNull();
    }

    @Test
    @DisplayName("연결이 끊겨도 유예 시간 안에는 방에 남아 있다.")
    void disconnectedMemberStaysInRoomWithinGrace() {
        Long roomId = host.createRoom("유예 확인용 방");
        guest.join(roomId);

        guest.disconnectWebSocket();

        assertThat(nicknamesOf(playersOf(host.readRoomState(roomId)))).contains("참가자");
    }

    @Test
    @DisplayName("유예가 만료되면 돌아오지 않은 사람만 방에서 빠지고, 돌아온 사람은 남는다.")
    void graceEvictsOnlyWhoeverDidNotComeBack() {
        Actor returner = signUpAndConnect("returneruser", "복귀자");
        try {
            Long roomId = host.createRoom("유예 만료 확인용 방");
            guest.join(roomId);
            returner.join(roomId);

            guest.disconnectWebSocket();
            returner.disconnectWebSocket();
            returner.reconnectWebSocket();

            await().atMost(LONGER_THAN_LEAVE_GRACE).untilAsserted(() ->
                    assertThat(nicknamesOf(playersOf(host.readRoomState(roomId))))
                            .containsExactlyInAnyOrder("방장", "복귀자"));
        } finally {
            returner.close();
        }
    }

    @Test
    @DisplayName("유예가 만료되면 끊긴 시점의 방이 아니라 그때 있는 방에서 내보낸다.")
    void graceEvictsFromTheRoomTheMemberIsInNow() {
        Long leftRoomId = host.createRoom("떠난 방");
        guest.join(leftRoomId);
        Long movedRoomId = host.createRoom("옮겨간 방");

        guest.disconnectWebSocket();
        guest.leave(leftRoomId);
        guest.join(movedRoomId);

        await().atMost(LONGER_THAN_LEAVE_GRACE).untilAsserted(() ->
                assertThat(nicknamesOf(playersOf(host.readRoomState(movedRoomId)))).containsExactly("방장"));
        assertThat(nicknamesOf(playersOf(host.readRoomState(leftRoomId)))).containsExactly("방장");
    }

    @Test
    @DisplayName("초대는 초대받은 사람에게만 간다.")
    void inviteReachesOnlyTheTarget() {
        Long roomId = host.createRoom("초대 확인용 방");
        host.subscribe(userQueue(StompDestination.INVITE));
        guest.subscribe(userQueue(StompDestination.INVITE));

        host.invite(roomId, guest.memberId);

        assertThat(guest.takeMap(userQueue(StompDestination.INVITE)).get("roomTitle")).isEqualTo("초대 확인용 방");
        assertThat(host.poll(userQueue(StompDestination.INVITE))).isNull();
    }

    @Test
    @DisplayName("채팅은 같은 방을 구독한 사람에게 간다.")
    void chatReachesRoomSubscribers() {
        Long roomId = host.createRoom("채팅 확인용 방");
        String roomTopic = StompDestination.room(roomId);
        guest.join(roomId);
        guest.subscribe(roomTopic);

        host.publishChat(roomId, "안녕");

        Map<String, Object> event = guest.takeMap(roomTopic);
        assertThat(event.get("type")).isEqualTo("CHAT");
        assertThat(event.get("message")).isEqualTo("안녕");
    }

    private Actor signUpAndConnect(String loginId, String nickname) {
        Long memberId = memberRepository.save(MemberEntity.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(PASSWORD))
                .nickname(nickname)
                .email(loginId + "@fun-game.club")
                .role(Role.USER)
                .build()).getId();

        return new Actor(memberId, login(loginId));
    }

    private String login(String loginId) {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("loginId", loginId, "password", PASSWORD), Map.class);

        String sessionCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(sessionCookie).as("로그인 응답에 세션 쿠키가 있어야 한다").isNotNull();

        return sessionCookie.split(";", 2)[0];
    }

    private static String userQueue(String destination) {
        return "/user" + destination;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> roomStateOf(Map<String, Object> event) {
        return (Map<String, Object>) event.get("room");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> playersOf(Map<String, Object> roomState) {
        return (List<Map<String, Object>>) roomState.get("players");
    }

    private static int versionOf(Map<String, Object> roomState) {
        return ((Number) roomState.get("version")).intValue();
    }

    private static Map<String, Object> roomOf(List<Map<String, Object>> rooms, Long roomId) {
        return rooms.stream()
                .filter(room -> roomId.intValue() == ((Number) room.get("roomId")).intValue())
                .findFirst()
                .orElse(null);
    }

    private static boolean hasPlayers(Map<String, Object> room, int players) {
        return ((Number) room.get("currentPlayers")).intValue() == players;
    }

    private static List<String> nicknamesOf(List<Map<String, Object>> members) {
        return members.stream().map(member -> (String) member.get("nickname")).toList();
    }

    private final class Actor {

        private final Long memberId;
        private final String sessionCookie;
        private StompSession stompSession;
        private final Map<String, BlockingQueue<Object>> payloadsByDestination = new ConcurrentHashMap<>();

        private Actor(Long memberId, String sessionCookie) {
            this.memberId = memberId;
            this.sessionCookie = sessionCookie;
            this.stompSession = openStompSession();
        }

        private StompSession openStompSession() {
            WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
            handshakeHeaders.add(HttpHeaders.COOKIE, sessionCookie);

            try {
                return stompClient.connectAsync("ws://localhost:" + port + "/ws-quiz/websocket",
                                handshakeHeaders, new StompSessionHandlerAdapter() {
                                })
                        .get(MESSAGE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("STOMP 연결에 실패했다", e);
            }
        }

        private void subscribe(String destination) {
            BlockingQueue<Object> payloads = payloadsByDestination
                    .computeIfAbsent(destination, key -> new LinkedBlockingQueue<>());

            stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                @SuppressWarnings("unchecked")
                public void handleFrame(StompHeaders headers, Object payload) {
                    Map<String, Object> response = (Map<String, Object>) payload;
                    if ("SUCCESS".equals(response.get("result")) && response.get("data") != null) {
                        payloads.add(response.get("data"));
                    }
                }
            });
            settleSubscription();
        }

        private void settleSubscription() {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> awaitRoomInLobby(Long roomId, Predicate<Map<String, Object>> matching) {
            long deadline = System.nanoTime() + MESSAGE_TIMEOUT.toNanos();

            while (System.nanoTime() < deadline) {
                Object pushedRooms = poll(StompDestination.LOBBY);
                if (pushedRooms == null) {
                    continue;
                }

                Map<String, Object> room = roomOf((List<Map<String, Object>>) pushedRooms, roomId);
                if (room != null && matching.test(room)) {
                    return room;
                }
            }

            throw new AssertionError("조건을 만족하는 방 " + roomId + " 가 방 목록으로 오지 않았다");
        }

        private Object poll(String destination) {
            try {
                return queueOf(destination).poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> takeMap(String destination) {
            return (Map<String, Object>) take(destination);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> takeList(String destination) {
            return (List<Map<String, Object>>) take(destination);
        }

        private Object take(String destination) {
            try {
                Object payload = queueOf(destination).poll(MESSAGE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
                assertThat(payload).as("%s 로 메시지가 와야 한다", destination).isNotNull();
                return payload;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        private BlockingQueue<Object> queueOf(String destination) {
            return payloadsByDestination.computeIfAbsent(destination, key -> new LinkedBlockingQueue<>());
        }

        private Long createRoom(String title) {
            Map<String, Object> request = Map.of(
                    "gameType", "SONG",
                    "title", title,
                    "maxPlayers", 8,
                    "category", "KPOP",
                    "totalRound", 5,
                    "difficulty", 0);

            return ((Number) dataOf(call(HttpMethod.POST, "/game/rooms", request))).longValue();
        }

        private void join(Long roomId) {
            call(HttpMethod.POST, "/game/rooms/" + roomId + "/join", null);
        }

        private void leave(Long roomId) {
            call(HttpMethod.POST, "/game/rooms/" + roomId + "/leave", null);
        }

        private void toggleReady(Long roomId) {
            call(HttpMethod.POST, "/game/rooms/" + roomId + "/ready", null);
        }

        private void invite(Long roomId, Long targetMemberId) {
            call(HttpMethod.POST, "/api/rooms/" + roomId + "/invites", Map.of("targetMemberId", targetMemberId));
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> readRoomState(Long roomId) {
            return (Map<String, Object>) dataOf(call(HttpMethod.GET, "/game/rooms/" + roomId + "/users", null));
        }

        private void publishChat(Long roomId, String message) {
            stompSession.send("/app/room/" + roomId + "/chat", Map.of("message", message));
        }

        private Map<String, Object> call(HttpMethod method, String path, Object body) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, sessionCookie);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response =
                    restTemplate.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful())
                    .as("%s %s 는 성공해야 한다: %s", method, path, response.getBody())
                    .isTrue();

            return response.getBody();
        }

        private Object dataOf(Map<String, Object> response) {
            assertThat(response.get("result")).isEqualTo("SUCCESS");
            return response.get("data");
        }

        private void reconnectWebSocket() {
            disconnectWebSocket();
            stompSession = openStompSession();
        }

        private void disconnectWebSocket() {
            if (stompSession.isConnected()) {
                stompSession.disconnect();
            }
        }

        private void close() {
            disconnectWebSocket();
        }
    }
}
