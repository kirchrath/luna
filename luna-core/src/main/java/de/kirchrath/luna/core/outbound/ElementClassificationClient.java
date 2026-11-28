package de.kirchrath.luna.core.outbound;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.config.PromptProperties;
import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.ClassificationResultDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

@Component
public class ElementClassificationClient {

    private final AreaChatClients areaChatClients;
    private final PromptProperties promptProperties;

    public ElementClassificationClient(AreaChatClients areaChatClients, PromptProperties promptProperties) {
        this.areaChatClients = areaChatClients;
        this.promptProperties = promptProperties;
    }

    /** Asks the provider of the life area, so that an input never leaves the provider chosen for its area. */
    public ClassificationResultDto classify(LifeArea area, String text, LunaElementTypeDto typeHint) {
        return areaChatClients.of(area).prompt()
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
