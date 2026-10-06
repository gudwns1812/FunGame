package com.fungame.songquiz.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fungame.songquiz.SongquizApplication;
import com.fungame.songquiz.api.websocket.StompDestination;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.ComputerScienceEntity;
import com.fungame.songquiz.storage.ComputerScienceRepository;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

class TwoInstanceAcceptanceTest {

    private static final String PASSWORD = "password1!";
    private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration UNTIL_FIRST_ROUND = Duration.ofSeconds(15);
    private static final Duration PAST_LEAVE_GRACE = Duration.ofSeconds(4);
    private static final int REDIS_PORT = 6379;

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withCommand("mysqld", "--skip-log-bin", "--skip-performance-schema")
            .withReuse(true);
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(REDIS_PORT)
            .withReuse(true);

    private static ConfigurableApplicationContext instanceA;
    private static ConfigurableApplicationContext instanceB;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String runId = UUID.randomUUID().toString().substring(0, 8);
    private Actor host;
    private Actor guest;

    @BeforeAll
    static void startTwoInstances() {
        MYSQL.start();
        REDIS.start();
        instanceA = startInstance("instance-a");
        instanceB = startInstance("instance-b");
    }

    @AfterAll
    static void stopInstances() {
        closeQuietly(instanceA);
        closeQuietly(instanceB);
    }

    private static ConfigurableApplicationContext startInstance(String instanceId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.port", 0);
        properties.put("management.server.port", 0);
        properties.put("app.instance-id", instanceId);
        properties.put("spring.datasource.url", MYSQL.getJdbcUrl());
        properties.put("spring.datasource.username", MYSQL.getUsername());
        properties.put("spring.datasource.password", MYSQL.getPassword());
        properties.put("spring.data.redis.host", REDIS.getHost());
        properties.put("spring.data.redis.port", REDIS.getMappedPort(REDIS_PORT));
        properties.put("spring.session.jdbc.initialize-schema", "always");
        properties.put("app.song-scrape.enabled", false);
        properties.put("app.room.leave-grace-seconds", 1);

        String[] commandLine = properties.entrySet().stream()
                .map(property -> "--" + property.getKey() + "=" + property.getValue())
                .toArray(String[]::new);

        return new SpringApplicationBuilder(SongquizApplication.class).run(commandLine);
    }

    private static void closeQuietly(ConfigurableApplicationContext context) {
        if (context != null && context.isActive()) {
            context.close();
        }
    }

    private static int portOf(ConfigurableApplicationContext context) {
        return ((ServletWebServerApplicationContext) context).getWebServer().getPort();
    }

    @BeforeEach
    void setUp() {
        host = signUp("host", portOf(instanceA));
        guest = signUp("guest", portOf(instanceB));
    }

    @AfterEach
    void tearDown() {
        host.disconnectWebSocket();
        guest.disconnectWebSocket();
    }

    @Test
    @DisplayName("A 에서 만든 방이 B 의 방 목록에 보이고, B 로 들어온 입장이 처리된다.")
    void roomFromAIsListedAndJoinableOnB() {
        Long roomId = host.createRoom("A 에서 만든 방");

        assertThat(roomIdsOf(guest.listRooms())).contains(roomId);

        guest.join(roomId);

        assertThat(nicknamesOf(playersOf(host.readRoomState(roomId))))
                .containsExactlyInAnyOrder(host.nickname, guest.nickname);
    }

    @Test
    @DisplayName("방 채팅이 A 와 B 의 구독자 모두에게 닿는다.")
    void roomChatReachesBothInstances() {
        Long roomId = host.createRoom("채팅 방");
        guest.join(roomId);
        host.subscribe(StompDestination.room(roomId));
        guest.subscribe(StompDestination.room(roomId));

        guest.publishChat(roomId, "B 에서 보낸 말");

        assertThat(host.awaitEvent(StompDestination.room(roomId), "CHAT").get("message")).isEqualTo("B 에서 보낸 말");
        assertThat(guest.awaitEvent(StompDestination.room(roomId), "CHAT").get("message")).isEqualTo("B 에서 보낸 말");
    }

    @Test
    @DisplayName("A 에서 끊긴 사람이 유예 안에 B 로 다시 붙으면 방에서 쫓겨나지 않는다.")
    void reconnectingToAnotherInstanceKeepsTheMemberInTheRoom() {
        Long roomId = host.createRoom("재접속 방");
        Actor wanderer = signUp("wanderer", portOf(instanceA));
        wanderer.join(roomId);

        wanderer.disconnectWebSocket();
        wanderer.connectTo(portOf(instanceB));

        await().pollDelay(PAST_LEAVE_GRACE).atMost(PAST_LEAVE_GRACE.plusSeconds(5)).untilAsserted(() ->
                assertThat(nicknamesOf(playersOf(host.readRoomState(roomId))))
                        .containsExactlyInAnyOrder(host.nickname, wanderer.nickname));
        wanderer.disconnectWebSocket();
    }

    @Test
    @DisplayName("A 에서 시작한 판의 정답을 B 로 보내면 맞힌 것으로 처리된다.")
    void answerOnBIsScoredForGameStartedOnA() {
        seedQuestions();
        Long roomId = host.createCsRoom("판 방");
        guest.join(roomId);
        guest.toggleReady(roomId);
        guest.subscribe(StompDestination.room(roomId));

        host.startGame(roomId);
        Map<String, Object> roundStart = guest.awaitEvent(StompDestination.room(roomId), "ROUND_START", UNTIL_FIRST_ROUND);
        guest.publishChat(roomId, answerOf(roundStart));

        Map<String, Object> roundEnd = guest.awaitEvent(StompDestination.room(roomId), "ROUND_END");
        assertThat(((Number) roundEnd.get("winnerMemberId")).longValue()).isEqualTo(guest.memberId);
    }

    @Test
    @DisplayName("판이 도는 중에 판을 시작한 서버가 죽어도 다른 서버에서 판이 이어지고 다음 라운드가 온다.")
    void gameContinuesWhenTheStartingInstanceDies() {
        seedQuestions();
        ConfigurableApplicationContext doomed = startInstance("instance-doomed");
        Actor starter = signUp("starter", portOf(instanceB));
        Actor player = signUp("player", portOf(instanceB));
        Long roomId = starter.createCsRoom("살아남는 판");
        player.join(roomId);
        player.toggleReady(roomId);
        player.subscribe(StompDestination.room(roomId));

        starter.startGameOn(portOf(doomed), roomId);
        doomed.close();

        Map<String, Object> firstRound = player.awaitEvent(StompDestination.room(roomId), "ROUND_START", UNTIL_FIRST_ROUND);
        player.publishChat(roomId, answerOf(firstRound));
        player.awaitEvent(StompDestination.room(roomId), "ROUND_END");
        Map<String, Object> secondRound = player.awaitEvent(StompDestination.room(roomId), "ROUND_START", UNTIL_FIRST_ROUND);

        assertThat(((Number) secondRound.get("round")).intValue()).isEqualTo(2);
        starter.disconnectWebSocket();
        player.disconnectWebSocket();
    }

    private void seedQuestions() {
        ComputerScienceRepository questions = instanceB.getBean(ComputerScienceRepository.class);
        for (int i = 0; i < 3; i++) {
            String key = runId + "-" + i;
            questions.save(ComputerScienceEntity.builder()
                    .field("OS")
                    .content("질문-" + key)
                    .answers(List.of("정답-" + key))
                    .explanation("해설")
                    .difficulty(CSQuizDifficulty.EASY)
                    .build());
        }
    }

    private static String answerOf(Map<String, Object> roundStart) {
        String content = (String) roundStart.get("content");
        String key = content.substring(content.indexOf("질문-") + "질문-".length());
        return "정답-" + key;
    }

    private Actor signUp(String role, int port) {
        String loginId = role + runId;
        String nickname = role + runId;
        MemberRepository members = instanceB.getBean(MemberRepository.class);
        PasswordEncoder passwordEncoder = instanceB.getBean(PasswordEncoder.class);
        Long memberId = members.save(MemberEntity.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(PASSWORD))
                .nickname(nickname)
                .email(loginId + "@fun-game.club")
                .role(Role.USER)
                .build()).getId();

        Actor actor = new Actor(memberId, nickname, login(port, loginId), port);
        actor.connectTo(port);
        return actor;
    }

    @SuppressWarnings("rawtypes")
    private String login(int port, String loginId) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                Map.of("loginId", loginId, "password", PASSWORD), Map.class);

        String sessionCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(sessionCookie).as("로그인 응답에 세션 쿠키가 있어야 한다").isNotNull();

        return sessionCookie.split(";", 2)[0];
    }

    @SuppressWarnings("unchecked")
    private static List<Long> roomIdsOf(Object rooms) {
        return ((List<Map<String, Object>>) rooms).stream()
                .map(room -> ((Number) room.get("roomId")).longValue())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> playersOf(Map<String, Object> roomState) {
        return (List<Map<String, Object>>) roomState.get("players");
    }

    private static List<String> nicknamesOf(List<Map<String, Object>> players) {
        return players.stream().map(player -> (String) player.get("nickname")).toList();
    }

    private final class Actor {

        private final Long memberId;
        private final String nickname;
        private final String sessionCookie;
        private final int httpPort;
        private final Map<String, BlockingQueue<Map<String, Object>>> eventsByDestination = new ConcurrentHashMap<>();
        private StompSession stompSession;

        private Actor(Long memberId, String nickname, String sessionCookie, int httpPort) {
            this.memberId = memberId;
            this.nickname = nickname;
            this.sessionCookie = sessionCookie;
            this.httpPort = httpPort;
        }

        private void connectTo(int port) {
            WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
            WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
            handshakeHeaders.add(HttpHeaders.COOKIE, sessionCookie);

            try {
                stompSession = stompClient.connectAsync("ws://localhost:" + port + "/ws-quiz/websocket",
                                handshakeHeaders, new StompSessionHandlerAdapter() {
                                })
                        .get(MESSAGE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("STOMP 연결에 실패했다", e);
            }
        }

        private void disconnectWebSocket() {
            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
            }
        }

        private void subscribe(String destination) {
            BlockingQueue<Map<String, Object>> events =
                    eventsByDestination.computeIfAbsent(destination, key -> new LinkedBlockingQueue<>());

            stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                @SuppressWarnings("unchecked")
                public void handleFrame(StompHeaders headers, Object payload) {
                    Map<String, Object> response = (Map<String, Object>) payload;
                    if (response.get("data") instanceof Map<?, ?> data) {
                        events.add((Map<String, Object>) data);
                    }
                }
            });
            settle();
        }

        private Map<String, Object> awaitEvent(String destination, String type) {
            return awaitEvent(destination, type, MESSAGE_TIMEOUT);
        }

        private Map<String, Object> awaitEvent(String destination, String type, Duration timeout) {
            long deadline = System.nanoTime() + timeout.toNanos();
            BlockingQueue<Map<String, Object>> events =
                    eventsByDestination.computeIfAbsent(destination, key -> new LinkedBlockingQueue<>());

            while (System.nanoTime() < deadline) {
                try {
                    Map<String, Object> event = events.poll(200, TimeUnit.MILLISECONDS);
                    if (event != null && type.equals(event.get("type"))) {
                        return event;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }

            throw new AssertionError(destination + " 로 " + type + " 이 오지 않았다");
        }

        private void publishChat(Long roomId, String message) {
            stompSession.send("/app/room/" + roomId + "/chat", Map.of("message", message));
        }

        private Long createRoom(String title) {
            return createRoom(Map.of("gameType", "SONG", "title", title, "maxPlayers", 8, "category", "KPOP",
                    "totalRound", 5, "difficulty", 0));
        }

        private Long createCsRoom(String title) {
            return createRoom(Map.of("gameType", "CS", "title", title, "maxPlayers", 8, "totalRound", 2,
                    "difficulty", 0, "csDifficulty", "HARD"));
        }

        private Long createRoom(Map<String, Object> request) {
            return ((Number) dataOf(call(httpPort, HttpMethod.POST, "/game/rooms", request))).longValue();
        }

        private Object listRooms() {
            return dataOf(call(httpPort, HttpMethod.GET, "/game/rooms", null));
        }

        private void join(Long roomId) {
            call(httpPort, HttpMethod.POST, "/game/rooms/" + roomId + "/join", null);
        }

        private void toggleReady(Long roomId) {
            call(httpPort, HttpMethod.POST, "/game/rooms/" + roomId + "/ready", null);
        }

        private void startGame(Long roomId) {
            startGameOn(httpPort, roomId);
        }

        private void startGameOn(int port, Long roomId) {
            call(port, HttpMethod.POST, "/game/rooms/" + roomId + "/start", null);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> readRoomState(Long roomId) {
            return (Map<String, Object>) dataOf(call(httpPort, HttpMethod.GET, "/game/rooms/" + roomId + "/users", null));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private Map<String, Object> call(int port, HttpMethod method, String path, Object body) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, sessionCookie);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange("http://localhost:" + port + path, method,
                    new HttpEntity<>(body, headers), Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful())
                    .as("%s %s 는 성공해야 한다: %s", method, path, response.getBody())
                    .isTrue();

            return response.getBody();
        }

        private Object dataOf(Map<String, Object> response) {
            assertThat(response.get("result")).isEqualTo("SUCCESS");
            return response.get("data");
        }

        private void settle() {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
