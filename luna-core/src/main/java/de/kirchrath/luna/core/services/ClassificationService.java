package de.kirchrath.luna.core.services;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.converter.LunaElementTypeConverter;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.ClassificationResultDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class ClassificationService {

    private final ChatClient chatClient;
    private final Resource classificationPrompt;
    private final InboundEventRepository inboundEventRepository;
    private final OutboundEventRepository outboundEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;
    private final ClassificationProperties classificationProperties;
    private final LunaElementTypeConverter lunaElementTypeConverter;

    public ClassificationService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/classification.st") Resource classificationPrompt,
            InboundEventRepository inboundEventRepository,
            OutboundEventRepository outboundEventRepository,
            TransactionTemplate transactionTemplate,
            JsonMapper jsonMapper,
            ClassificationProperties classificationProperties,
            LunaElementTypeConverter lunaElementTypeConverter) {
        this.chatClient = chatClientBuilder.build();
        this.classificationPrompt = classificationPrompt;
        this.inboundEventRepository = inboundEventRepository;
        this.outboundEventRepository = outboundEventRepository;
        this.transactionTemplate = transactionTemplate;
        this.jsonMapper = jsonMapper;
        this.classificationProperties = classificationProperties;
        this.lunaElementTypeConverter = lunaElementTypeConverter;
    }

    public void classify(InboundEvent inboundEvent) {
        Set<LunaElementType> types;
        try {
            types = determineTypes(jsonMapper.readValue(inboundEvent.data().json(), ManualInputDataDto.class));
        } catch (RuntimeException e) {
            recordFailedAttempt(inboundEvent, e);
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            types.forEach(type -> outboundEventRepository.save(
                    new OutboundEvent(null, inboundEvent.id(), type, OutboundEventStatus.CLASSIFIED, null, null, null)));
            inboundEventRepository.save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
        });
        log.debug("Classified inbound event {} as {}", inboundEvent.id(), types);
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

    private void recordFailedAttempt(InboundEvent inboundEvent, RuntimeException cause) {
        var attempts = inboundEvent.attempts() + 1;
        var maxAttempts = classificationProperties.maxAttempts();
        if (attempts >= maxAttempts) {
            log.error("Classification of inbound event {} failed finally after {} attempts",
                    inboundEvent.id(), attempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.FAILED));
        } else {
            log.warn("Classification of inbound event {} failed (attempt {}/{})",
                    inboundEvent.id(), attempts, maxAttempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.RECEIVED));
        }
    }
}
