package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Message;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.MessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MessagePostDTO;

public class MessageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private MessageService messageService;

    private User user;
    private Event event;
    private MessagePostDTO messagePostDTO;

    private static final Long EVENT_ID = 10L;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setToken("valid-token");

        event = new Event();
        event.setId(EVENT_ID);
        event.setParticipants(new ArrayList<>(List.of(user)));

        messagePostDTO = new MessagePostDTO();
        messagePostDTO.setToken("valid-token");
        messagePostDTO.setEventId(EVENT_ID);
        messagePostDTO.setContent("Ciao a tutti!");
    }

    // --- saveMessage ---

    @Test
    void saveMessage_validInput_success() {
        when(userRepository.findByToken("valid-token")).thenReturn(user);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Message result = messageService.saveMessage(messagePostDTO);

        assertNotNull(result);
        assertEquals("Ciao a tutti!", result.getContent());
        assertEquals(user, result.getSender());
        assertEquals(event, result.getEvent());
        assertNotNull(result.getTimestamp());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void saveMessage_invalidToken_throws401() {
        when(userRepository.findByToken("valid-token")).thenReturn(null);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> messageService.saveMessage(messagePostDTO)
        );

        assertEquals(401, ex.getStatusCode().value());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void saveMessage_eventNotFound_throws404() {
        when(userRepository.findByToken("valid-token")).thenReturn(user);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> messageService.saveMessage(messagePostDTO)
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void saveMessage_userNotParticipant_throws403() {
        event.setParticipants(new ArrayList<>());
        when(userRepository.findByToken("valid-token")).thenReturn(user);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> messageService.saveMessage(messagePostDTO)
        );

        assertEquals(403, ex.getStatusCode().value());
        verify(messageRepository, never()).save(any());
    }

    // --- getChatHistory ---

    @Test
    void getChatHistory_validParticipant_returnsMessages() {
        Message m1 = new Message();
        m1.setContent("primo");
        Message m2 = new Message();
        m2.setContent("secondo");

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(messageRepository.findByEventIdOrderByTimestampAsc(EVENT_ID)).thenReturn(List.of(m1, m2));

        List<Message> result = messageService.getChatHistory(10L, user);

        assertEquals(2, result.size());
        assertEquals("primo", result.get(0).getContent());
        assertEquals("secondo", result.get(1).getContent());
    }

    @Test
    void getChatHistory_userNotParticipant_throws403() {
        event.setParticipants(new ArrayList<>());
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> messageService.getChatHistory(10L, user)
        );

        assertEquals(403, ex.getStatusCode().value());
        verify(messageRepository, never()).findByEventIdOrderByTimestampAsc(any());
    }

    @Test
    void getChatHistory_eventNotFound_throws404() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> messageService.getChatHistory(10L, user)
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(messageRepository, never()).findByEventIdOrderByTimestampAsc(any());
    }

    // --- grace period tests ---

    @Test
    void saveMessage_eventCancelledWithinGracePeriod_success() {
        event.setCancelledAt(LocalDateTime.now().minusHours(1));
        when(userRepository.findByToken("valid-token")).thenReturn(user);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Message result = messageService.saveMessage(messagePostDTO);

        assertNotNull(result);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void saveMessage_eventCancelledAfterGracePeriod_throws404() {
        event.setCancelledAt(LocalDateTime.now().minusHours(25));
        when(userRepository.findByToken("valid-token")).thenReturn(user);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> messageService.saveMessage(messagePostDTO)
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getChatHistory_eventCancelledWithinGracePeriod_returnsMessages() {
        event.setCancelledAt(LocalDateTime.now().minusHours(1));
        Message m = new Message();
        m.setContent("ultimo messaggio");

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(messageRepository.findByEventIdOrderByTimestampAsc(EVENT_ID)).thenReturn(List.of(m));

        List<Message> result = messageService.getChatHistory(EVENT_ID, user);

        assertEquals(1, result.size());
        assertEquals("ultimo messaggio", result.get(0).getContent());
    }

    @Test
    void getChatHistory_eventCancelledAfterGracePeriod_throws404() {
        event.setCancelledAt(LocalDateTime.now().minusHours(25));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> messageService.getChatHistory(EVENT_ID, user)
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(messageRepository, never()).findByEventIdOrderByTimestampAsc(any());
    }
}
