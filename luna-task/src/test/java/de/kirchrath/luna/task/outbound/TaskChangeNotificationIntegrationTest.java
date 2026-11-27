package de.kirchrath.luna.task.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.kirchrath.luna.task.config.TestcontainersConfig;
import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskPriority;
import de.kirchrath.luna.task.data.entity.TaskSource;
import de.kirchrath.luna.task.data.entity.TaskStatus;
import de.kirchrath.luna.task.data.repository.TaskRepository;
import de.kirchrath.luna.task.dto.TaskChangedDto;
import de.kirchrath.luna.task.services.TaskApplicationService;

// As in the prod profile, behind Traefik.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=https://luna.internal")
@Import(TestcontainersConfig.class)
class TaskChangeNotificationIntegrationTest {

    private static final String TOKEN = "test-token";
    private static final String TOPIC = "/topic/tasks";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Autowired
    private TaskApplicationService taskApplicationService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(TOKEN)).thenReturn(
                Jwt.withTokenValue(TOKEN).header("alg", "none").subject("chandra").build());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
    }

    @Test
    void thatCompletingATaskShouldResultIntoASignalToSubscribedClients() throws Exception {
        // given
        var task = taskRepository.save(Task.builder()
                .source(TaskSource.CORE)
                .sourceId(UUID.randomUUID())
                .title("Pay the electricity bill")
                .priority(TaskPriority.NORMAL)
                .status(TaskStatus.OPEN)
                .edited(false)
                .build());
        var session = connect("ws://localhost:" + port + "/ws?access_token=" + TOKEN);
        var signals = subscribe(session);

        // when
        taskApplicationService.completeTask(task.id());

        // then
        var signal = signals.poll(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(signal).isEqualTo(new TaskChangedDto(task.id()));
    }

    @Test
    void thatConnectingWithoutTokenShouldResultIntoARejectedHandshake() {
        // given
        var url = "ws://localhost:" + port + "/ws";

        // when / then
        assertThatThrownBy(() -> connect(url)).isInstanceOf(ExecutionException.class).hasStackTraceContaining("401");
    }

    @Test
    void thatAHandshakeForwardedByTraefikFromTheAllowedOriginShouldResultIntoAConnectedSession() throws Exception {
        // given
        // Traefik forwards the WebSocket handshake with X-Forwarded-Proto wss, which never matches the https origin.
        var headers = new WebSocketHttpHeaders();
        headers.setOrigin("https://luna.internal");
        headers.add("X-Forwarded-Proto", "wss");
        headers.add("X-Forwarded-Host", "luna.internal");

        // when
        var session = stompClient.connectAsync("ws://localhost:" + port + "/ws?access_token=" + TOKEN, headers,
                new StompSessionHandlerAdapter() {
                }).get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        // then
        assertThat(session.isConnected()).isTrue();
    }

    private StompSession connect(String url) throws Exception {
        return stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
        }).get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private BlockingQueue<TaskChangedDto> subscribe(StompSession session) throws InterruptedException {
        var signals = new LinkedBlockingQueue<TaskChangedDto>();
        session.subscribe(TOPIC, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TaskChangedDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                signals.add((TaskChangedDto) payload);
            }
        });
        // The subscription is registered asynchronously; signals sent before would be lost.
        var deadline = Instant.now().plus(TIMEOUT);
        while (simpUserRegistry.findSubscriptions(subscription -> TOPIC.equals(subscription.getDestination())).isEmpty()
                && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
        }
        return signals;
    }
}
