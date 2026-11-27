package de.kirchrath.luna.alert.outbound;

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

import de.kirchrath.luna.alert.config.TestcontainersConfig;
import de.kirchrath.luna.alert.data.entity.Alert;
import de.kirchrath.luna.alert.data.entity.AlertSeverity;
import de.kirchrath.luna.alert.data.entity.AlertSource;
import de.kirchrath.luna.alert.data.entity.AlertStatus;
import de.kirchrath.luna.alert.data.repository.AlertRepository;
import de.kirchrath.luna.alert.dto.AlertChangedDto;
import de.kirchrath.luna.alert.services.AlertApplicationService;

// As in the prod profile, behind Traefik.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=https://luna.internal")
@Import(TestcontainersConfig.class)
class AlertChangeNotificationIntegrationTest {

    private static final String TOKEN = "test-token";
    private static final String TOPIC = "/topic/alerts";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Autowired
    private AlertApplicationService alertApplicationService;

    @Autowired
    private AlertRepository alertRepository;

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
    void thatAcknowledgingAnAlertShouldResultIntoASignalToSubscribedClients() throws Exception {
        // given
        var alert = alertRepository.save(Alert.builder()
                .source(AlertSource.CORE)
                .sourceId(UUID.randomUUID())
                .title("NAS disk almost full")
                .message("Volume 1 is 94 % full")
                .severity(AlertSeverity.CRITICAL)
                .status(AlertStatus.OPEN)
                .build());
        var session = connect("ws://localhost:" + port + "/ws?access_token=" + TOKEN);
        var signals = subscribe(session);

        // when
        alertApplicationService.acknowledgeAlert(alert.id());

        // then
        var signal = signals.poll(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(signal).isEqualTo(new AlertChangedDto(alert.id()));
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

    private BlockingQueue<AlertChangedDto> subscribe(StompSession session) throws InterruptedException {
        var signals = new LinkedBlockingQueue<AlertChangedDto>();
        session.subscribe(TOPIC, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AlertChangedDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                signals.add((AlertChangedDto) payload);
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
