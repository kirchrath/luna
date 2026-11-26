package de.kirchrath.luna.note.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.note.config.TestcontainersConfig;
import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteSource;
import de.kirchrath.luna.note.data.entity.NoteStatus;
import de.kirchrath.luna.note.data.repository.CoreNoteProjectionRepository;
import de.kirchrath.luna.note.data.repository.NoteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private CoreNoteProjectionRepository projectionRepository;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void thatListingNotesShouldResultIntoTheNewestFirst() throws Exception {
        // given
        var older = save("Owners meeting", NoteStatus.ACTIVE);
        var newer = save("Pumpkin soup", NoteStatus.ARCHIVED);

        // when / then
        mockMvc.perform(get("/notes").param("page", "0").param("size", "10").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(newer.id().toString(), older.id().toString())))
                .andExpect(jsonPath("$.items[0].title").value("Pumpkin soup"))
                .andExpect(jsonPath("$.items[0].summary").value("Roof will be renovated in May"))
                .andExpect(jsonPath("$.items[0].status").value("ARCHIVED"))
                .andExpect(jsonPath("$.items[0].edited").value(false))
                .andExpect(jsonPath("$.items[0].createdDate").isNotEmpty())
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    void thatListingNotesByStatusShouldResultIntoTheMatchingNotesOnly() throws Exception {
        // given
        var active = save("Owners meeting", NoteStatus.ACTIVE);
        save("Pumpkin soup", NoteStatus.ARCHIVED);

        // when / then
        mockMvc.perform(get("/notes").param("status", "ACTIVE").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains(active.id().toString())))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void thatListingNotesWithoutTokenShouldResultIntoUnauthorized() throws Exception {
        // when / then
        mockMvc.perform(get("/notes")).andExpect(status().isUnauthorized());
    }

    @Test
    void thatGettingANoteShouldResultIntoItsDataAndAudit() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ACTIVE);

        // when / then
        mockMvc.perform(get("/notes/{id}", note.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Owners meeting"))
                .andExpect(jsonPath("$.data.summary").value("Roof will be renovated in May"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.archivedDate").doesNotExist())
                .andExpect(content().string(not(containsString(":null"))))
                .andExpect(jsonPath("$.data.source").value("CORE"))
                .andExpect(jsonPath("$.data.sourceId").value(note.sourceId().toString()))
                .andExpect(jsonPath("$.data.edited").value(false))
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatGettingAnUnknownNoteShouldResultIntoNotFound() throws Exception {
        // when / then
        mockMvc.perform(get("/notes/{id}", UUID.randomUUID()).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void thatUpdatingANoteShouldResultIntoAnEditedNote() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ACTIVE);

        // when / then
        mockMvc.perform(put("/notes/{id}", note.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Owners meeting 2026", "summary": "Next meeting in October", "version": 0}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Owners meeting 2026"))
                .andExpect(jsonPath("$.data.summary").value("Next meeting in October"))
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.audit.version").value(1));
        assertThat(noteRepository.findById(note.id())).get().extracting(Note::title).isEqualTo("Owners meeting 2026");
    }

    @Test
    void thatUpdatingANoteInAnOutdatedVersionShouldResultIntoAConflict() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ACTIVE);

        // when / then
        mockMvc.perform(put("/notes/{id}", note.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Owners meeting 2026", "summary": "Next meeting in October", "version": 3}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void thatUpdatingANoteWithoutSummaryShouldResultIntoABadRequest() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ACTIVE);

        // when / then
        mockMvc.perform(put("/notes/{id}", note.id()).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Owners meeting", "summary": " ", "version": 0}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thatArchivingAnActiveNoteShouldResultIntoAnArchivedNoteWithDate() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ACTIVE);

        // when / then
        mockMvc.perform(post("/notes/{id}/archive", note.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.archivedDate").isNotEmpty())
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.audit.version").value(1));
    }

    @Test
    void thatArchivingAnArchivedNoteShouldResultIntoAnUnchangedNote() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ARCHIVED);

        // when / then
        mockMvc.perform(post("/notes/{id}/archive", note.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    @Test
    void thatRestoringAnArchivedNoteShouldResultIntoAnActiveNoteWithoutDate() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ARCHIVED);

        // when / then
        mockMvc.perform(post("/notes/{id}/restore", note.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.archivedDate").doesNotExist())
                .andExpect(jsonPath("$.audit.version").value(1));
    }

    @Test
    void thatRestoringAnActiveNoteShouldResultIntoAnUnchangedNote() throws Exception {
        // given
        var note = save("Owners meeting", NoteStatus.ACTIVE);

        // when / then
        mockMvc.perform(post("/notes/{id}/restore", note.id()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.version").value(0));
    }

    private Note save(String title, NoteStatus status) {
        return noteRepository.save(Note.builder()
                .source(NoteSource.CORE)
                .sourceId(UUID.randomUUID())
                .title(title)
                .summary("Roof will be renovated in May")
                .status(status)
                .edited(false)
                .build());
    }
}
