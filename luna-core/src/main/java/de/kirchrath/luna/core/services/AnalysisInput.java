package de.kirchrath.luna.core.services;

import java.time.OffsetDateTime;

import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

/**
 * What the analysis of an inbound event works with, whatever its source.
 *
 * @param area          decides which AI provider analyzes the input
 * @param referenceTime relative dates such as "tomorrow" refer to it
 */
public record AnalysisInput(LifeArea area, String text, LunaElementTypeDto typeHint, OffsetDateTime referenceTime) {
}
