package de.kirchrath.luna.note.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteStatus;
import de.kirchrath.luna.note.dto.AuditDto;
import de.kirchrath.luna.note.dto.NoteDataDto;
import de.kirchrath.luna.note.dto.NoteDto;
import de.kirchrath.luna.note.dto.NoteEventDto;
import de.kirchrath.luna.note.dto.NoteListItemDto;
import de.kirchrath.luna.note.dto.NoteStatusDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteMapper {

    NoteDataDto toDataDto(Note note);

    AuditDto toAuditDto(Note note);

    @Mapping(target = "data", source = "note")
    @Mapping(target = "audit", source = "note")
    NoteDto toDto(Note note);

    @Mapping(target = "audit", source = "note")
    @Mapping(target = "data", source = "note")
    NoteEventDto toEventDto(Note note);

    NoteListItemDto toListItemDto(Note note);

    NoteStatus toEntity(NoteStatusDto status);

}
