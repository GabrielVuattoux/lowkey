package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class ChatChannelInterceptorTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final EventRepository eventRepository = Mockito.mock(EventRepository.class);
    private final MessageChannel channel = Mockito.mock(MessageChannel.class);
    private final ChatChannelInterceptor interceptor = new ChatChannelInterceptor(userRepository, eventRepository);

    private final Principal user1 = () -> "1";

    @BeforeEach
    public void setup() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByToken("valid-token")).thenReturn(user);
        when(eventRepository.existsByIdAndParticipants_Id(5L, 1L)).thenReturn(true);
        when(eventRepository.existsByIdAndParticipants_Id(6L, 1L)).thenReturn(false);
    }

    private Message<byte[]> frame(StompCommand command, Principal user, String destination, String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (user != null) {
            accessor.setUser(user);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    public void connect_validToken_setsSessionUser() {
        Message<byte[]> message = frame(StompCommand.CONNECT, null, null, "Bearer valid-token");

        interceptor.preSend(message, channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        assertEquals("1", accessor.getUser().getName());
    }

    @Test
    public void connect_withoutToken_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), channel));
    }

    @Test
    public void connect_invalidToken_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, "Bearer nope"), channel));
    }

    @Test
    public void subscribe_participant_isAllowed() {
        assertDoesNotThrow(
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, user1, "/topic/chat/5", null), channel));
    }

    @Test
    public void subscribe_nonParticipant_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, user1, "/topic/chat/6", null), channel));
    }

    @Test
    public void subscribe_withoutConnectUser_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/topic/chat/5", null), channel));
    }

    @Test
    public void subscribe_otherDestination_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, user1, "/topic/other", null), channel));
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, user1, "/topic/chat/abc", null), channel));
    }

    @Test
    public void send_throughApp_isAllowed() {
        assertDoesNotThrow(
                () -> interceptor.preSend(frame(StompCommand.SEND, user1, "/app/chat/5", null), channel));
    }

    @Test
    public void send_straightToTopic_isRejected() {
        // Would broadcast to the chat without the checks in MessageController
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SEND, user1, "/topic/chat/5", null), channel));
    }
}
