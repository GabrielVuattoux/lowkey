package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.EventService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicEventController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    // Needed by the @AuthenticatedUser argument resolver, which the public endpoint does not use
    @MockitoBean
    private UserRepository userRepository;

    private Event publicEvent() {
        User creator = new User();
        creator.setId(1L);
        creator.setUsername("alice");
        User participant = new User();
        participant.setId(2L);

        Event event = new Event();
        event.setId(7L);
        event.setTitle("Board games night");
        event.setDescription("Bring your favourite game");
        event.setCategory(EventCategory.SOCIAL);
        event.setStartTime(LocalDateTime.of(2026, 10, 1, 19, 0));
        event.setEndTime(LocalDateTime.of(2026, 10, 1, 22, 0));
        event.setLatitude(47.3764);
        event.setLongitude(8.5480);
        event.setIsPrivate(false);
        event.setCreator(creator);
        event.setParticipants(List.of(creator, participant));
        event.setInviteCode("SECRET12");
        return event;
    }

    @Test
    public void getPublicEvents_withoutAuthHeader_returnsPublicFieldsOnly() throws Exception {
        given(eventService.getPublicEventsInCells(List.of("u0qjd"))).willReturn(List.of(publicEvent()));

        mockMvc.perform(get("/public/events").param("cells", "u0qjd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(7)))
                .andExpect(jsonPath("$[0].title", is("Board games night")))
                .andExpect(jsonPath("$[0].category", is("SOCIAL")))
                .andExpect(jsonPath("$[0].startTime", is("2026-10-01T19:00:00")))
                .andExpect(jsonPath("$[0].participantCount", is(2)))
                .andExpect(jsonPath("$[0].inviteCode").doesNotExist())
                .andExpect(jsonPath("$[0].creatorId").doesNotExist())
                .andExpect(jsonPath("$[0].creatorUsername").doesNotExist())
                .andExpect(jsonPath("$[0].participantIds").doesNotExist())
                .andExpect(jsonPath("$[0].isPrivate").doesNotExist());
    }

    @Test
    public void getPublicEvents_commaSeparatedCells_passesEveryCell() throws Exception {
        given(eventService.getPublicEventsInCells(anyList())).willReturn(List.of());

        mockMvc.perform(get("/public/events").param("cells", "u0qjd,u0qje"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(eventService).getPublicEventsInCells(List.of("u0qjd", "u0qje"));
    }

    @Test
    public void getPublicEvents_missingCells_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/public/events"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getPublicEvents_invalidCells_returnsBadRequest() throws Exception {
        given(eventService.getPublicEventsInCells(anyList()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid cells"));

        mockMvc.perform(get("/public/events").param("cells", "u0qjd1"))
                .andExpect(status().isBadRequest());
    }
}
