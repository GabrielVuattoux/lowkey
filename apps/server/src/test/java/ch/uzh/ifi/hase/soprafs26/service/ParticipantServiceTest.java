package ch.uzh.ifi.hase.soprafs26.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;

public class ParticipantServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventService eventService; // used for getEventById()

    @InjectMocks
    private ParticipantService service; // replace with actual class name

    private Event event;
    private User user;
    private User eventCreator;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);

        eventCreator = new User();
        eventCreator.setId(2L);

        event = new Event();
        event.setId(1L);
        event.setCreator(eventCreator);
        event.setParticipants(new ArrayList<>());
        event.setIsPrivate(false);
    }

    @Test
    void removeParticipantTest() {
        event.getParticipants().add(user);

        when(eventService.getEventById(1L)).thenReturn(event);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.removeParticipantFromEvent(1L, 1L);

        assertFalse(event.getParticipants().contains(user));
        verify(eventRepository).save(event);
    }

    @Test
    void creatorCannotLeaveEventTest() {
        // the user gets now the creator
        event.getParticipants().add(user);
        event.getParticipants().add(eventCreator);

        when(eventService.getEventById(1L)).thenReturn(event);
        when(userRepository.findById(2L)).thenReturn(Optional.of(eventCreator));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.removeParticipantFromEvent(2L, 1L)
        );

        assertEquals(403, exception.getStatusCode().value());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void joinEventByIdTest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(event);
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = service.joinEventById(1L, 1L);

        assertNotNull(result);
        assertTrue(result.getParticipants().contains(user));
        assertEquals(1, result.getParticipants().size());

        verify(eventRepository).save(event);
    }

    @Test
    void joinEventByInviteCode_validInput_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByInviteCode("ABC123")).thenReturn(event);
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Event result = service.joinEventByInviteCode("ABC123", 1L);

        assertNotNull(result);
        assertTrue(result.getParticipants().contains(user));
        verify(eventRepository).save(event);
    }

    @Test
    void joinEventByInviteCode_eventNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByInviteCode("INVALID")).thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.joinEventByInviteCode("INVALID", 1L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void joinEventByInviteCode_alreadyJoined_throwsConflict() {
        event.getParticipants().add(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByInviteCode("ABC123")).thenReturn(event);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.joinEventByInviteCode("ABC123", 1L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void joinEventByInviteCode_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.joinEventByInviteCode("ABC123", 99L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void joinEventById_pastEvent_throwsForbidden() {
        event.setEndTime(LocalDateTime.now().minusHours(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(event);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.joinEventById(1L, 1L));
        assertEquals(403, ex.getStatusCode().value());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void joinEventByInviteCode_pastEvent_throwsForbidden() {
        event.setEndTime(LocalDateTime.now().minusHours(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByInviteCode("ABC123")).thenReturn(event);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.joinEventByInviteCode("ABC123", 1L));
        assertEquals(403, ex.getStatusCode().value());
        verify(eventRepository, never()).save(any());
    }

}
