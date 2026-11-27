package de.kirchrath.luna.appointment.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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

import de.kirchrath.luna.appointment.config.TestcontainersConfig;
import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.data.entity.AppointmentSource;
import de.kirchrath.luna.appointment.data.entity.AppointmentStatus;
import de.kirchrath.luna.appointment.data.entity.LifeArea;
import de.kirchrath.luna.appointment.data.repository.AppointmentRepository;
import de.kirchrath.luna.appointment.dto.AppointmentChangedDto;
import de.kirchrath.luna.appointment.services.AppointmentApplicationService;

// As in the prod profile, behind Traefik.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=https://luna.internal")
@Import(TestcontainersConfig.class)
class AppointmentChangeNotificationIntegrationTest {

    private static final String TOKEN = "test-token";
    private static final String TOPIC = "/topic/appointments";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Autowired
    private AppointmentApplicationService appointmentApplicationService;

    @Autowired
    private AppointmentRepository appointmentRepository;

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
    void thatCancellingAnAppointmentShouldResultIntoASignalToSubscribedClients() throws Exception {
        // given
        var appointment = appointmentRepository.save(Appointment.builder()
                .source(AppointmentSource.CORE)
                .sourceId(UUID.randomUUID())
                .area(LifeArea.PERSONAL)
                .title("Dentist")
                .start(OffsetDateTime.parse("2026-12-01T10:00:00+01:00"))
                .effectiveEnd(OffsetDateTime.parse("2026-12-01T10:00:00+01:00"))
                .status(AppointmentStatus.SCHEDULED)
                .edited(false)
                .build());
        var session = connect("ws://localhost:" + port + "/ws?access_token=" + TOKEN);
        var signals = subscribe(session);

        // when
        appointmentApplicationService.cancelAppointment(appointment.id());

        // then
        var signal = signals.poll(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(signal).isEqualTo(new AppointmentChangedDto(appointment.id()));
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

    private BlockingQueue<AppointmentChangedDto> subscribe(StompSession session) throws InterruptedException {
        var signals = new LinkedBlockingQueue<AppointmentChangedDto>();
        session.subscribe(TOPIC, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AppointmentChangedDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                signals.add((AppointmentChangedDto) payload);
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
