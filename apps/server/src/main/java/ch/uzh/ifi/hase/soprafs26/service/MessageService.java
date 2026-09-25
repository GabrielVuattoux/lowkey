package ch.uzh.ifi.hase.soprafs26.service;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Message;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.MessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MessagePostDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class MessageService {
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final EventRepository eventRepository;

    public MessageService(@Qualifier("userRepository") UserRepository userRepository, @Qualifier("messageRepository") MessageRepository messageRepository, @Qualifier("eventRepository") EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.eventRepository = eventRepository;
    }

    private User getUserByToken(String token){
        User userByToken = userRepository.findByToken(token);
        if (userByToken == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return userByToken;
    }

    private Event getEventIfAllowed(Long eventId, User user){
        Event eventByEventId = eventRepository.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("event with id %d does not exist", eventId)));

        if (eventByEventId.getCancelledAt() != null) {
            LocalDateTime gracePeriodEnd = eventByEventId.getCancelledAt().plusHours(ch.uzh.ifi.hase.soprafs26.service.EventService.CHAT_GRACE_HOURS);
            if (LocalDateTime.now(ZoneId.of("Europe/Zurich")).isAfter(gracePeriodEnd)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("event with id %d is no longer available", eventId));
            }
        }

        if(!eventByEventId.getParticipants().contains(user)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user is not allowed to post a message to this event, not a participant");
        }

        return eventByEventId;
    }

    public Message saveMessage(MessagePostDTO messagePostDTO){
        User userByToken = getUserByToken(messagePostDTO.getToken());
        Long eventId = messagePostDTO.getEventId();

        Event eventByEventId = getEventIfAllowed(eventId, userByToken);
        Message message = new Message();
        message.setContent(messagePostDTO.getContent());
        message.setSender(userByToken);
        message.setEvent(eventByEventId);
        message.setTimestamp(LocalDateTime.now(ZoneId.of("Europe/Zurich")));

        messageRepository.save(message);

        return message;
    }

    public List<Message> getChatHistory(Long eventId, User user) {
        getEventIfAllowed(eventId, user);
        return messageRepository.findByEventIdOrderByTimestampAsc(eventId);
    }


}
