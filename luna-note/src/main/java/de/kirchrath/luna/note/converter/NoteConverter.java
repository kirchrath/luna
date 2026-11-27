package de.kirchrath.luna.note.converter;

import java.util.Set;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.note.data.entity.LifeArea;
import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteSource;
import de.kirchrath.luna.note.data.entity.NoteStatus;
import de.kirchrath.luna.note.dto.LifeAreaDto;
import de.kirchrath.luna.note.dto.NoteDto;
import de.kirchrath.luna.note.dto.NoteEventDto;
import de.kirchrath.luna.note.dto.NoteListItemDto;
import de.kirchrath.luna.note.dto.NoteStatusDto;
import de.kirchrath.luna.note.dto.NoteUpdateDto;
import de.kirchrath.luna.note.dto.core.NoteEventDataDto;

@Component
public class NoteConverter {

    private final NoteMapper noteMapper;

    public NoteConverter(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    /** A new, unedited and active note from a note event of luna-core. */
    public Note fromCore(de.kirchrath.luna.note.dto.core.NoteEventDto event) {
        return applyCore(Note.builder()
                .source(NoteSource.CORE)
                .sourceId(event.getId())
                .area(noteMapper.toEntity(event.getArea()))
                .status(NoteStatus.ACTIVE)
                .edited(false)
                .build(), event);
    }

    /** The data of a newer note event of luna-core; status, origin and audit stay. */
    public Note applyCore(Note note, de.kirchrath.luna.note.dto.core.NoteEventDto event) {
        NoteEventDataDto data = event.getData();
        return note.toBuilder()
                .title(data.getTitle())
                .summary(data.getSummary())
                .build();
    }

    /** The data edited by the user; the note counts as edited from now on. */
    public Note applyUpdate(Note note, NoteUpdateDto update) {
        return note.toBuilder()
                .title(update.title())
                .summary(update.summary())
                .edited(true)
                .build();
    }

    public NoteDto toDto(Note note) {
        return noteMapper.toDto(note);
    }

    public NoteEventDto toEventDto(Note note) {
        return noteMapper.toEventDto(note);
    }

    public NoteListItemDto toListItemDto(Note note) {
        return noteMapper.toListItemDto(note);
    }

    public NoteStatus toEntity(NoteStatusDto status) {
        return noteMapper.toEntity(status);
    }

    /** Only the given area, or all areas without one. */
    public Set<LifeArea> toEntities(LifeAreaDto area) {
        return area == null ? Set.of(LifeArea.values()) : Set.of(noteMapper.toEntity(area));
    }
}
