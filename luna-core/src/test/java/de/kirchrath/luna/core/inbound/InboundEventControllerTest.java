package de.kirchrath.luna.core.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.services.InboundEventApplicationService;
import de.kirchrath.luna.core.services.TimeProvider;
import de.kirchrath.luna.core.services.UuidProvider;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        "app.classification.interval=1h",
        // A provider exported in the shell (e.g. SPRING_AI_MODEL_CHAT for the stack) must not replace the mock.
        "spring.ai.model.chat=none"})
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class InboundEventControllerTest {

    private static final UUID ID = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-24T15:30:00+01:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InboundEventRepository inboundEventRepository;

    @Autowired
    private OutboundEventRepository outboundEventRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UuidProvider uuidProvider;

    @MockitoBean
    private TimeProvider timeProvider;

    @MockitoSpyBean
    private InboundEventApplicationService inboundEventApplicationService;

    @BeforeEach
    void setUp() {
        outboundEventRepository.deleteAll();
        inboundEventRepository.deleteAll();
        when(uuidProvider.randomUuid()).thenReturn(ID);
        when(timeProvider.now()).thenReturn(NOW);
        when(timeProvider.atServerOffset(NOW.toInstant())).thenReturn(NOW);
    }

    @Test
    void thatSubmittingAManualInputShouldResultIntoAnAcceptedAndPersistedInboundEvent() throws Exception {
        // given
        var input = new ManualInputDataDto("Dentist appointment next Tuesday at 10", LunaElementTypeDto.APPOINTMENT);

        // when
        var result = mockMvc.perform(post("/inbound-events/manual")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(input)));

        // then
        result.andExpect(status().isAccepted()).andExpect(jsonPath("$.id").value(ID.toString()));
        var stored = inboundEventRepository.findById(ID).orElseThrow();
        assertThat(stored.source()).isEqualTo(EventSource.MANUAL);
        assertThat(stored.status()).isEqualTo(InboundEventStatus.RECEIVED);
        assertThat(stored.attempts()).isZero();
        assertThat(jsonMapper.readValue(stored.data().json(), ManualInputDataDto.class)).isEqualTo(input);
        assertThat(stored.createdDate()).isEqualTo(NOW);
        assertThat(stored.lastModifiedDate()).isEqualTo(NOW);
        assertThat(stored.version()).isZero();
    }

    @Test
    void thatSubmittingAManualInputWithoutTypeHintShouldResultIntoAnAcceptedInboundEvent() throws Exception {
        // given
        var content = "{\"text\": \"Milk is empty\"}";

        // when
        var result = mockMvc.perform(post("/inbound-events/manual")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));

        // then
        result.andExpect(status().isAccepted());
        var stored = inboundEventRepository.findById(ID).orElseThrow();
        assertThat(jsonMapper.readValue(stored.data().json(), ManualInputDataDto.class))
                .isEqualTo(new ManualInputDataDto("Milk is empty", null));
    }

    @Test
    void thatSubmittingABlankTextShouldResultIntoBadRequest() throws Exception {
        // given
        var content = "{\"text\": \"   \"}";

        // when
        var result = mockMvc.perform(post("/inbound-events/manual")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));

        // then
        result.andExpect(status().isBadRequest());
        assertThat(inboundEventRepository.count()).isZero();
    }

    @Test
    void thatSubmittingATextLongerThanTenThousandCharactersShouldResultIntoBadRequest() throws Exception {
        // given
        var input = new ManualInputDataDto("x".repeat(10_001), null);

        // when
        var result = mockMvc.perform(post("/inbound-events/manual")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(input)));

        // then
        result.andExpect(status().isBadRequest());
        assertThat(inboundEventRepository.count()).isZero();
    }

    @Test
    void thatSubmittingAnUnknownTypeHintShouldResultIntoBadRequest() throws Exception {
        // given
        var content = "{\"text\": \"Something\", \"typeHint\": \"UNKNOWN\"}";

        // when
        var result = mockMvc.perform(post("/inbound-events/manual")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));

        // then
        result.andExpect(status().isBadRequest());
        assertThat(inboundEventRepository.count()).isZero();
    }

    @Test
    void thatSubmittingAManualInputWithoutTokenShouldResultIntoUnauthorized() throws Exception {
        // given
        var input = new ManualInputDataDto("Something", null);

        // when
        var result = mockMvc.perform(post("/inbound-events/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(input)));

        // then
        result.andExpect(status().isUnauthorized());
        assertThat(inboundEventRepository.count()).isZero();
    }

    @Test
    void thatListingManualInputsShouldResultIntoAPageNewestFirstWithStatusAndElements() throws Exception {
        // given
        var oldestId = UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11");
        var middleId = UUID.fromString("9a4c2e1b-8d7f-4e3a-b2c1-0f9e8d7c6b5a");
        var newestId = UUID.fromString("3c5d7e9f-1a2b-4c3d-8e4f-5a6b7c8d9e0f");
        var taskId = UUID.fromString("7e8f9a0b-1c2d-4e3f-9a4b-5c6d7e8f9a0b");
        when(uuidProvider.randomUuid()).thenReturn(oldestId, middleId, newestId, taskId);
        when(timeProvider.now()).thenReturn(NOW, NOW.plusMinutes(1), NOW.plusMinutes(2), NOW.plusMinutes(3));
        when(timeProvider.atServerOffset(any(Instant.class)))
                .thenAnswer(invocation -> invocation.<Instant>getArgument(0).atOffset(ZoneOffset.ofHours(1)));
        saveManualInput("Pay the electricity bill", LunaElementTypeDto.TASK, InboundEventStatus.ANALYZED);
        saveManualInput("Idea: plant the balcony", null, InboundEventStatus.FAILED);
        saveManualInput("Milk is empty", null, InboundEventStatus.RECEIVED);
        outboundEventRepository.save(new OutboundEvent(
                null, oldestId, LunaElementType.TASK, OutboundEventStatus.PUBLISHED, null, null, null));

        // when
        var firstPage = mockMvc.perform(get("/inbound-events/manual").param("page", "0").param("size", "2").with(jwt()));
        var secondPage = mockMvc.perform(get("/inbound-events/manual").param("page", "1").param("size", "2").with(jwt()));

        // then
        firstPage.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(newestId.toString()))
                .andExpect(jsonPath("$.items[0].text").value("Milk is empty"))
                .andExpect(jsonPath("$.items[0].typeHint").doesNotExist())
                .andExpect(content().string(not(containsString(":null"))))
                .andExpect(jsonPath("$.items[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].elements").isEmpty())
                .andExpect(jsonPath("$.items[1].id").value(middleId.toString()))
                .andExpect(jsonPath("$.items[1].status").value("FAILED"));
        secondPage.andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(oldestId.toString()))
                .andExpect(jsonPath("$.items[0].typeHint").value("TASK"))
                .andExpect(jsonPath("$.items[0].status").value("ANALYZED"))
                .andExpect(jsonPath("$.items[0].createdDate").value("2026-11-24T15:30:00+01:00"))
                .andExpect(jsonPath("$.items[0].elements[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.items[0].elements[0].type").value("TASK"));
    }

    @Test
    void thatListingManualInputsWithoutPagingParametersShouldResultIntoTheFirstPageWithTheDefaultSize() throws Exception {
        // given
        var request = get("/inbound-events/manual").with(jwt());

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalCount").value(0));
        verify(inboundEventApplicationService).listManualInboundEvents(PageRequest.of(0, 20));
    }

    @Test
    void thatListingManualInputsWithATooLargePageSizeShouldResultIntoTheMaximumPageSize() throws Exception {
        // given
        var request = get("/inbound-events/manual").param("size", "500").with(jwt());

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isOk());
        verify(inboundEventApplicationService).listManualInboundEvents(PageRequest.of(0, 100));
    }

    @Test
    void thatListingManualInputsWithoutTokenShouldResultIntoUnauthorized() throws Exception {
        // given
        var request = get("/inbound-events/manual");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isUnauthorized());
    }

    private void saveManualInput(String text, LunaElementTypeDto typeHint, InboundEventStatus status) {
        inboundEventRepository.save(new InboundEvent(
                null,
                EventSource.MANUAL,
                new EventData(jsonMapper.writeValueAsString(new ManualInputDataDto(text, typeHint))),
                status,
                0,
                null,
                null,
                null));
    }

    @Test
    void thatListingManualInputsWithATokenAsQueryParameterShouldResultIntoUnauthorized() throws Exception {
        // given
        var request = get("/inbound-events/manual").param("access_token", "some-token");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void thatARequestForwardedByTraefikFromTheOwnHttpsOriginShouldResultIntoNoCorsRejection() throws Exception {
        // given
        // Behind Traefik the service is called via http, while the browser sends its https origin.
        var request = post("/inbound-events/manual").contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"Milk is empty\"}")
                .with(jwt())
                .header(HttpHeaders.ORIGIN, "https://luna.internal")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "luna.internal");

        // when
        var result = mockMvc.perform(request);

        // then
        result.andExpect(status().isAccepted());
    }
}
