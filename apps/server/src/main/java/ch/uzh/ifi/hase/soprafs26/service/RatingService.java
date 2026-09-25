package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Rating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RatingRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final EventRepository eventRepository;

    public RatingService(@Qualifier("ratingRepository") RatingRepository ratingRepository,
                         @Qualifier("eventRepository") EventRepository eventRepository) {
        this.ratingRepository = ratingRepository;
        this.eventRepository = eventRepository;
    }

    public Rating createRating(Long eventId, User rater, Integer score) {
        if (score == null || score < 1 || score > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score must be between 1 and 5");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event has not ended yet");
        }

        if (event.getCreator().getId().equals(rater.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Event creator cannot rate themselves");
        }

        boolean isParticipant = event.getParticipants() != null
                && event.getParticipants().stream().anyMatch(p -> p.getId().equals(rater.getId()));
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can rate the organizer");
        }

        if (ratingRepository.existsByRaterAndEvent(rater, event)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already rated this event");
        }

        Rating rating = new Rating();
        rating.setRater(rater);
        rating.setOrganizer(event.getCreator());
        rating.setEvent(event);
        rating.setScore(score);
        rating.setCreatedAt(LocalDateTime.now());
        return ratingRepository.save(rating);
    }

    public Rating getMyRating(User rater, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return ratingRepository.findByRaterAndEvent(rater, event);
    }

    public double getAverageRating(User organizer) {
        List<Rating> ratings = ratingRepository.findByOrganizer(organizer);
        if (ratings.isEmpty()) return 0.0;
        return ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
    }

    public int getRatingCount(User organizer) {
        return ratingRepository.findByOrganizer(organizer).size();
    }
}