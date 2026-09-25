package ch.uzh.ifi.hase.soprafs26.service;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.server.ResponseStatusException;


@Service
@Transactional
public class ParticipantService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private final EventService eventService;

    public ParticipantService(@Qualifier("eventRepository") EventRepository eventRepository, @Qualifier("userRepository") UserRepository userRepository, EventService eventService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    public Event joinEventById(Long eventId, Long userId) {
        User userFromId = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Event event = eventRepository.findById((long) eventId);
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        if (event.getIsPrivate()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot join a private event without an invite code");
        }
        if (event.getEndTime() != null && event.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot join an event that has already ended");
        }
        if (event.getParticipants().contains(userFromId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already joined the event");
        }
        event.getParticipants().add(userFromId);
        return eventRepository.save(event);
    }

    public Event joinEventByInviteCode(String inviteCode, Long userId) {
        User userFromId = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Event event = eventRepository.findByInviteCode(inviteCode);
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        if (event.getEndTime() != null && event.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot join an event that has already ended");
        }
        if (event.getParticipants().contains(userFromId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already joined the event");
        }
        event.getParticipants().add(userFromId);
        return eventRepository.save(event);
    }

    public void removeParticipantFromEvent(long userId, Long eventId) {
        Event event = eventService.getEventById(eventId);
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event to be deleted could not be found");
        }
        User user = userRepository.findById(userId)
        
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User to be deleted could not be found"));
        

        List<User> participants = event.getParticipants();
        if (!participants.contains(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a participant of the event");
        }

        if (event.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Event creator cannot leave the event");
        }

        participants.remove(user);
        eventRepository.save(event);

    }


    /// helper functions ///

}
