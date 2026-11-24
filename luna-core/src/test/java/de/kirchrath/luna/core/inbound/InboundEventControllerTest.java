package de.kirchrath.luna.core.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.core.config.TestcontainersConfig;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.dto.LunaElementType;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.services.TimeProvider;
import de.kirchrath.luna.core.services.UuidProvider;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class InboundEventControllerTest {

    private static final UUID ID = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-24T15:30:00+01:00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InboundEventRepository inboundEventRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UuidProvider uuidProvider;

    @MockitoBean
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        inboundEventRepository.deleteAll();
        when(uuidProvider.randomUuid()).thenReturn(ID);
        when(timeProvider.now()).thenReturn(NOW);
        when(timeProvider.atServerOffset(NOW.toInstant())).thenReturn(NOW);
    }

    @Test
    void thatSubmittingAManualInputShouldResultIntoAnAcceptedAndPersistedInboundEvent() throws Exception {
        // given
        var input = new ManualInputDataDto("Dentist appointment next Tuesday at 10", LunaElementType.APPOINTMENT);

        // when
        var result = mockMvc.perform(post("/inbound-events/manual")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(input)));

        // then
        result.andExpect(status().isAccepted()).andExpect(jsonPath("$.id").value(ID.toString()));
        var stored = inboundEventRepository.findById(ID).orElseThrow();
        assertThat(stored.source()).isEqualTo(EventSource.MANUAL);
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
}
