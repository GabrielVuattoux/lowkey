package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;   
import org.springframework.http.HttpStatus;

import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;


public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EventService eventService;

    private User creator;
    private Event newEvent;
    private Event event;
    
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");

        newEvent = new Event();
        newEvent.setTitle("Test Event");
        newEvent.setStartTime(LocalDateTime.now().plusDays(7));
        newEvent.setEndTime(LocalDateTime.now().plusDays(14));

        event = new Event();
        event.setId(10L);
        event.setTitle("Test Event");
        event.setCreator(creator);

        

        when(eventRepository.findByInviteCode(any())).thenReturn(null);
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createEvent_validTimesTest() {
        Event result = eventService.createEvent(newEvent, creator);

        assertEquals(creator, result.getCreator());

        assertNotNull(result.getInviteCode());

        verify(eventRepository).save(newEvent);
    }

    @Test
    void createEvent_invalidTimeTest() {
        newEvent.setStartTime(LocalDateTime.now().plusDays(14));
        newEvent.setEndTime(LocalDateTime.now().plusDays(7));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(newEvent, creator)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("End time must be after start time", exception.getReason());

        // ensure nothing is saved
        verify(eventRepository, never()).save(any());
    }

    @Test
    void getEventsByUserId_includesCancelledEvents() {
        Event cancelledEvent = new Event();
        cancelledEvent.setId(20L);
        cancelledEvent.setTitle("Cancelled Event");
        cancelledEvent.setCancelledAt(LocalDateTime.now().minusHours(1));

        when(eventRepository.findByParticipantId(1L)).thenReturn(List.of(event, cancelledEvent));

        List<Event> result = eventService.getEventsByUserId(1L);

        assertEquals(2, result.size());
        assertNotNull(result.get(1).getCancelledAt());
    }

    @Test
    void deleteEventTest() {
        Long eventId = 10L;

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.deleteEvent(eventId, creator);

        verify(eventRepository, never()).delete(any());
        verify(eventRepository).save(event);
        assertNotNull(event.getCancelledAt());
        verify(messagingTemplate).convertAndSend(eq("/topic/events/10/cancelled"), any(Object.class));
    }

    @Test
    void getEventById_found_returnsEvent() {
        when(eventRepository.findById(10L)).thenReturn(event);

        Event result = eventService.getEventById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void getEventById_notFound_throwsNotFound() {
        when(eventRepository.findById(99L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.getEventById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateEvent_validInput_success() {
        Long eventId = 10L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Event updates = new Event();
        updates.setTitle("Updated Title");
        updates.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        updates.setEndTime(LocalDateTime.of(2026, 5, 2, 10, 0));

        Event result = eventService.updateEvent(eventId, updates, creator);

        assertEquals("Updated Title", result.getTitle());
        verify(eventRepository).save(event);
    }

    @Test
    void updateEvent_notCreator_throwsForbidden() {
        Long eventId = 10L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        User otherUser = new User();
        otherUser.setId(99L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEvent(eventId, new Event(), otherUser));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateEvent_invalidTime_throwsBadRequest() {
        Long eventId = 10L;
        event.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        event.setEndTime(LocalDateTime.of(2026, 5, 2, 10, 0));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Event updates = new Event();
        updates.setStartTime(LocalDateTime.of(2026, 5, 3, 10, 0));
        updates.setEndTime(LocalDateTime.of(2026, 5, 1, 10, 0));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEvent(eventId, updates, creator));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getEventParticipants_returnsParticipants() {
        User participant = new User();
        participant.setId(5L);
        event.setParticipants(List.of(participant));
        when(eventRepository.findById(10L)).thenReturn(event);

        List<User> result = eventService.getEventParticipants(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getId());
    }

    @Test
    void getEventParticipants_noParticipants_returnsEmptyList() {
        event.setParticipants(null);
        when(eventRepository.findById(10L)).thenReturn(event);

        List<User> result = eventService.getEventParticipants(10L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

}
