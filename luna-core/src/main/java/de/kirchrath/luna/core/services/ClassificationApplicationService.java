package de.kirchrath.luna.core.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import de.kirchrath.luna.core.converter.LunaElementTypeConverter;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.dto.ClassificationResultDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ClassificationApplicationService {

    private final ClassificationDomainService classificationDomainService;
    private final ChatClient chatClient;
    private final Resource classificationPrompt;
    private final JsonMapper jsonMapper;
    private final LunaElementTypeConverter lunaElementTypeConverter;

    public ClassificationApplicationService(
            ClassificationDomainService classificationDomainService,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/classification.st") Resource classificationPrompt,
            JsonMapper jsonMapper,
            LunaElementTypeConverter lunaElementTypeConverter) {
        this.classificationDomainService = classificationDomainService;
        this.chatClient = chatClientBuilder.build();
        this.classificationPrompt = classificationPrompt;
        this.jsonMapper = jsonMapper;
        this.lunaElementTypeConverter = lunaElementTypeConverter;
    }

    public List<InboundEvent> findInboundEventsToClassify() {
        return classificationDomainService.findInboundEventsToClassify();
    }

    public void classify(InboundEvent inboundEvent) {
        classificationDomainService.completeClassification(inboundEvent, determineOutcome(inboundEvent));
    }

    private ClassificationOutcome determineOutcome(InboundEvent inboundEvent) {
        try {
            var input = jsonMapper.readValue(inboundEvent.data().json(), ManualInputDataDto.class);
            return new ClassificationOutcome(determineTypes(input), null);
        } catch (RuntimeException e) {
            return new ClassificationOutcome(Set.of(), e);
        }
    }

    private Set<LunaElementType> determineTypes(ManualInputDataDto input) {
        var result = chatClient.prompt()
                .user(user -> user.text(classificationPrompt)
                        .param("text", input.text())
                        .param("typeHint", typeHintInstruction(input.typeHint())))
                .call()
                .entity(ClassificationResultDto.class);

        var types = new LinkedHashSet<LunaElementType>();
        if (input.typeHint() != null) {
            types.add(lunaElementTypeConverter.toEntity(input.typeHint()));
        }
        if (result != null && result.types() != null) {
            result.types().forEach(type -> types.add(lunaElementTypeConverter.toEntity(type)));
        }
        return types;
    }

    private String typeHintInstruction(LunaElementTypeDto typeHint) {
        return typeHint == null
                ? ""
                : "The user marked this input as " + typeHint + ". Always include this type and add further types only if the input clearly requires them.";
    }
}
