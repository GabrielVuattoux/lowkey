package ch.uzh.ifi.hase.soprafs26.service;
import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Set;


@Service
@Transactional
public class EventService {
    //private final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public EventService(@Qualifier("eventRepository") EventRepository eventRepository, SimpMessagingTemplate messagingTemplate) {
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }


    public Event createEvent(Event newEvent, User user) {
        if (newEvent.getStartTime() != null
                && newEvent.getStartTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time cannot be in the past");
        }
        if (newEvent.getEndTime() != null && newEvent.getStartTime() != null
                && !newEvent.getEndTime().isAfter(newEvent.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
            List<User> participants = new ArrayList<>();
            // add creator to participants list
            participants.add(user);

            // get the participants that are already sent in the request
            List<User> existingParticipants = newEvent.getParticipants();
            if (existingParticipants != null && !existingParticipants.isEmpty()){
                participants.addAll(existingParticipants);
            }

            // set participants to be all the participants including the creator
            newEvent.setParticipants(participants);
        newEvent.setCreator(user);

        String inviteCode;
        do {
            inviteCode = generateInviteCode();
        } while (eventRepository.findByInviteCode(inviteCode) != null); // ensure invite code is unique
        newEvent.setInviteCode(inviteCode);
        
        return eventRepository.save(newEvent);
    } 

    public Event getEventById(Long id) {
        Event event = eventRepository.findById((long) id);
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        return event;
    }

    public List<Event> getEventsInRadius(double latitude, double longitude, double radiusKm, User user, Set<EventCategory> categories, boolean includePast) {
        List<Event> allEvents = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        return allEvents.stream()
            .filter(event -> event.getCancelledAt() == null)
            .filter(event -> {
                double distance = calculateDistance(latitude, longitude, event.getLatitude(), event.getLongitude());
                return distance <= radiusKm;
            })
            // `includePast` is a toggle: true → past events only (endTime in the
            // past), false → upcoming/ongoing only (endTime still in the future).
            // It is NOT "include past in addition to future".
            .filter(event -> includePast
                    ? !event.getEndTime().isAfter(now)
                    : event.getEndTime().isAfter(now))
            .filter(event -> {
                if (!event.getIsPrivate()) return true;
                return event.getParticipants() != null && event.getParticipants().contains(user);
            })
            .filter(event -> categories == null || categories.isEmpty() || categories.contains(event.getCategory()))
            .toList();
    }

    public Event updateEvent(Long eventId, Event updatedEvent, User user) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Event not found"));

        if (!existingEvent.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the creator can update the event");
        }

        
        LocalDateTime start = updatedEvent.getStartTime() != null
                ? updatedEvent.getStartTime() : existingEvent.getStartTime();

        LocalDateTime end = updatedEvent.getEndTime() != null
                ? updatedEvent.getEndTime() : existingEvent.getEndTime();

        if (start != null && end != null && !end.isAfter(start)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        if (updatedEvent.getTitle() != null) {
            existingEvent.setTitle(updatedEvent.getTitle());
        }

        if (updatedEvent.getDescription() != null) {
            existingEvent.setDescription(updatedEvent.getDescription());
        }

        if (updatedEvent.getStartTime() != null) {
            existingEvent.setStartTime(updatedEvent.getStartTime());
        }

        if (updatedEvent.getEndTime() != null) {
            existingEvent.setEndTime(updatedEvent.getEndTime());
        }

        if (updatedEvent.getLongitude() != null) {
            existingEvent.setLongitude(updatedEvent.getLongitude());
        }

        if (updatedEvent.getLatitude() != null) {
            existingEvent.setLatitude(updatedEvent.getLatitude());
        }

        if (updatedEvent.getIsPrivate() != null) {
            existingEvent.setIsPrivate(updatedEvent.getIsPrivate());
        }

        if (updatedEvent.getCategory() != null) {
            existingEvent.setCategory(updatedEvent.getCategory());
        }

        if (updatedEvent.getPictureUrls() != null) {
            existingEvent.setPictureUrls(updatedEvent.getPictureUrls());
        }

        return eventRepository.save(existingEvent);
}


    public List<Event> getEventsByUserId(Long userId) {
        return eventRepository.findByParticipantId(userId);
    }

    static final int CHAT_GRACE_HOURS = 24;

    public void deleteEvent(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        if (!event.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete the event");
        }
        event.setCancelledAt(LocalDateTime.now(ZoneId.of("Europe/Zurich")));
        eventRepository.save(event);

        Map<String, Object> payload = Map.of("eventId", eventId, "title", event.getTitle());
        messagingTemplate.convertAndSend("/topic/events/" + eventId + "/cancelled", (Object) payload);
    }

    @Scheduled(fixedRate = 3_600_000)
    public void cleanupCancelledEvents() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Europe/Zurich")).minusHours(CHAT_GRACE_HOURS);
        List<Event> expired = eventRepository.findByCancelledAtIsNotNullAndCancelledAtBefore(cutoff);
        eventRepository.deleteAll(expired);
    }

    public List<User> getEventParticipants(Long eventId) {
        Event event = getEventById(eventId);

        if (event.getParticipants() == null) {
            return new ArrayList<>();
        }

        return event.getParticipants();
    }


    /// helper functions ///
    
    // Haversine Formula to calculate distance between two points on the Earth (needed in getEventsInRadius)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // helper function to generate random invite code for events (used in createEvent)
    private String generateInviteCode() {
        // Generate a random alphanumeric string of length 8
        int length = 8;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder inviteCode = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            inviteCode.append(characters.charAt(index));
        }
        return inviteCode.toString();
    }


}
