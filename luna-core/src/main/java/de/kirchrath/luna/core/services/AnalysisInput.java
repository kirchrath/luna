package de.kirchrath.luna.core.services;

import java.time.OffsetDateTime;

import de.kirchrath.luna.core.dto.LunaElementTypeDto;

/**
 * What the analysis of an inbound event works with, whatever its source.
 *
 * @param referenceTime relative dates such as "tomorrow" refer to it
 */
public record AnalysisInput(String text, LunaElementTypeDto typeHint, OffsetDateTime referenceTime) {
}
