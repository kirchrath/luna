package de.kirchrath.luna.core.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
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

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.InboundEventChangedDto;
import de.kirchrath.luna.core.services.ClassificationScheduler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.classification.interval=1h")
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class InboundEventNotificationIntegrationTest {

    private static final String TOKEN = "test-token";
    private static final String TOPIC = "/topic/inbound-events";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Autowired
    private ClassificationScheduler classificationScheduler;

    @Autowired
    private InboundEventRepository inboundEventRepository;

    @Autowired
    private OutboundEventRepository outboundEventRepository;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @Autowired
    private ChatModel chatModel;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

    @BeforeEach
    void setUp() {
        outboundEventRepository.deleteAll();
        inboundEventRepository.deleteAll();
        reset(chatModel);
        ChatModelTestConfig.stubOptions(chatModel);
        when(jwtDecoder.decode(TOKEN)).thenReturn(
                Jwt.withTokenValue(TOKEN).header("alg", "none").subject("chandra").build());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
    }

    @Test
    void thatAFinallyFailedClassificationShouldResultIntoASignalToSubscribedClients() throws Exception {
        // given
        var inboundEvent = inboundEventRepository.save(new InboundEvent(
                null, EventSource.MANUAL, null, new EventData("{\"text\": \"Something\"}"), InboundEventStatus.RECEIVED, 2,
                null, null, null));
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider unavailable"));
        var session = connect("ws://localhost:" + port + "/ws?access_token=" + TOKEN);
        var signals = subscribe(session);

        // when
        classificationScheduler.classifyReceivedEvents();

        // then
        var signal = signals.poll(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(signal).isEqualTo(new InboundEventChangedDto(inboundEvent.id()));
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

    private BlockingQueue<InboundEventChangedDto> subscribe(StompSession session) throws InterruptedException {
        var signals = new LinkedBlockingQueue<InboundEventChangedDto>();
        session.subscribe(TOPIC, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return InboundEventChangedDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                signals.add((InboundEventChangedDto) payload);
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
