package de.kirchrath.luna.note.outbound;

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
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.kirchrath.luna.note.config.TestcontainersConfig;
import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteSource;
import de.kirchrath.luna.note.data.entity.NoteStatus;
import de.kirchrath.luna.note.data.repository.NoteRepository;
import de.kirchrath.luna.note.dto.NoteChangedDto;
import de.kirchrath.luna.note.services.NoteApplicationService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class NoteChangeNotificationIntegrationTest {

    private static final String TOKEN = "test-token";
    private static final String TOPIC = "/topic/notes";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Autowired
    private NoteApplicationService noteApplicationService;

    @Autowired
    private NoteRepository noteRepository;

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
    void thatArchivingANoteShouldResultIntoASignalToSubscribedClients() throws Exception {
        // given
        var note = noteRepository.save(Note.builder()
                .source(NoteSource.CORE)
                .sourceId(UUID.randomUUID())
                .title("Owners meeting")
                .summary("Roof will be renovated in May")
                .status(NoteStatus.ACTIVE)
                .edited(false)
                .build());
        var session = connect("ws://localhost:" + port + "/ws?access_token=" + TOKEN);
        var signals = subscribe(session);

        // when
        noteApplicationService.archiveNote(note.id());

        // then
        var signal = signals.poll(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(signal).isEqualTo(new NoteChangedDto(note.id()));
    }

    @Test
    void thatConnectingWithoutTokenShouldResultIntoARejectedHandshake() {
        // given
        var url = "ws://localhost:" + port + "/ws";

        // when / then
        assertThatThrownBy(() -> connect(url)).isInstanceOf(ExecutionException.class).hasStackTraceContaining("401");
    }

    private StompSession connect(String url) throws Exception {
        return stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
        }).get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private BlockingQueue<NoteChangedDto> subscribe(StompSession session) throws InterruptedException {
        var signals = new LinkedBlockingQueue<NoteChangedDto>();
        session.subscribe(TOPIC, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NoteChangedDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                signals.add((NoteChangedDto) payload);
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
