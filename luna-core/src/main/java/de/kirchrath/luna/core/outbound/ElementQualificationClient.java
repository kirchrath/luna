package de.kirchrath.luna.core.outbound;

import java.time.OffsetDateTime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.dto.QualificationResultDto;

@Component
public class ElementQualificationClient {

    private final ChatClient chatClient;

    public ElementQualificationClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public <D> QualificationResultDto<D> qualify(
            Resource prompt,
            String text,
            OffsetDateTime referenceTime,
            ParameterizedTypeReference<QualificationResultDto<D>> resultType) {
        return chatClient.prompt()
                .user(user -> user.text(prompt)
                        .param("text", text)
                        .param("referenceTime", referenceTime.toString()))
                .call()
                .entity(resultType);
    }
}
