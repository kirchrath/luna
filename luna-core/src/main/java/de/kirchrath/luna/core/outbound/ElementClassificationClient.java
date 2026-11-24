package de.kirchrath.luna.core.outbound;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.dto.ClassificationResultDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

@Component
public class ElementClassificationClient {

    private final ChatClient chatClient;
    private final Resource classificationPrompt;

    public ElementClassificationClient(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/classification.st") Resource classificationPrompt) {
        this.chatClient = chatClientBuilder.build();
        this.classificationPrompt = classificationPrompt;
    }

    public ClassificationResultDto classify(String text, LunaElementTypeDto typeHint) {
        return chatClient.prompt()
                .user(user -> user.text(classificationPrompt)
                        .param("text", text)
                        .param("typeHint", typeHintInstruction(typeHint)))
                .call()
                .entity(ClassificationResultDto.class);
    }

    private String typeHintInstruction(LunaElementTypeDto typeHint) {
        return typeHint == null
                ? ""
                : "The user marked this input as " + typeHint + ". Always include this type and add further types only if the input clearly requires them.";
    }
}
