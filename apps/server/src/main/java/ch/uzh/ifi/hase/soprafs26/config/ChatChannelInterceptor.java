package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Guards the STOMP chat:
 * - CONNECT needs a valid "Authorization: Bearer <token>" header;
 * - SUBSCRIBE is only allowed to the live chat of events the user participates in;
 * - SEND only goes through /app (MessageController checks it), never straight to a /topic.
 */
@Component
public class ChatChannelInterceptor implements ChannelInterceptor {

    static final String CHAT_TOPIC_PREFIX = "/topic/chat/";
    private static final String APP_PREFIX = "/app/";

    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public ChatChannelInterceptor(@Qualifier("userRepository") UserRepository userRepository,
                                  @Qualifier("eventRepository") EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            // Spring keeps this user on the session for all later frames
            case CONNECT -> accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> checkSubscription(accessor.getUser(), accessor.getDestination());
            case SEND -> checkSend(accessor.getDestination());
            default -> {
            }
        }
        return message;
    }

    private Principal authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessagingException("A bearer token is required to connect");
        }
        User user = userRepository.findByToken(authHeader.substring(7));
        if (user == null) {
            throw new MessagingException("Invalid token");
        }
        String userId = String.valueOf(user.getId());
        return () -> userId;
    }

    private void checkSubscription(Principal user, String destination) {
        if (user == null) {
            throw new MessagingException("Not authenticated");
        }
        if (destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
            throw new MessagingException("Unknown destination");
        }
        long eventId;
        try {
            eventId = Long.parseLong(destination.substring(CHAT_TOPIC_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new MessagingException("Unknown destination");
        }
        if (!eventRepository.existsByIdAndParticipants_Id(eventId, Long.valueOf(user.getName()))) {
            throw new MessagingException("Only participants can follow this chat");
        }
    }

    private void checkSend(String destination) {
        if (destination == null || !destination.startsWith(APP_PREFIX)) {
            throw new MessagingException("Messages must be sent through /app");
        }
    }
}
