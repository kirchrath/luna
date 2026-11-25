package de.kirchrath.luna.core.outbound;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.config.PromptProperties;
import de.kirchrath.luna.core.dto.ClassificationResultDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

@Component
public class ElementClassificationClient {

    private final ChatClient chatClient;
    private final PromptProperties promptProperties;

    public ElementClassificationClient(
            ChatClient.Builder chatClientBuilder, PromptProperties promptProperties) {
        this.chatClient = chatClientBuilder.build();
        this.promptProperties = promptProperties;
    }

    public ClassificationResultDto classify(String text, LunaElementTypeDto typeHint) {
        return chatClient.prompt()
                .user(user -> user.text(promptProperties.classification())
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
