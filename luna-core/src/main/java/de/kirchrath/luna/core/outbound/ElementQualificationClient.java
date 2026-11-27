package de.kirchrath.luna.core.outbound;

import java.time.OffsetDateTime;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.QualificationResultDto;

@Component
public class ElementQualificationClient {

    private final AreaChatClients areaChatClients;

    public ElementQualificationClient(AreaChatClients areaChatClients) {
        this.areaChatClients = areaChatClients;
    }

    /** Asks the provider of the life area, so that an input never leaves the provider chosen for its area. */
    public <D> QualificationResultDto<D> qualify(
            LifeArea area,
            Resource prompt,
            String text,
            OffsetDateTime referenceTime,
            ParameterizedTypeReference<QualificationResultDto<D>> resultType) {
        return areaChatClients.of(area).prompt()
                .user(user -> user.text(prompt)
                        .param("text", text)
                        .param("referenceTime", referenceTime.toString()))
                .call()
                .entity(resultType);
    }
}
