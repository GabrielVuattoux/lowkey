package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Rating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RatingService ratingService;

    private User organizer;
    private User participant;
    private Event event;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        organizer = new User();
        organizer.setId(1L);
        organizer.setUsername("organizer");

        participant = new User();
        participant.setId(2L);
        participant.setUsername("participant");

        event = new Event();
        event.setId(10L);
        event.setCreator(organizer);
        event.setEndTime(LocalDateTime.now().minusHours(1));

        List<User> participants = new ArrayList<>();
        participants.add(participant);
        event.setParticipants(participants);
    }

    @Test
    public void createRating_validInput_success() {
        Long eventId = 10L;
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Rating saved = new Rating();
        saved.setId(100L);
        saved.setRater(participant);
        saved.setOrganizer(organizer);
        saved.setEvent(event);
        saved.setScore(4);
        saved.setCreatedAt(LocalDateTime.now());

        Mockito.when(ratingRepository.save(Mockito.any(Rating.class))).thenReturn(saved);

        Rating result = ratingService.createRating(eventId, participant, 4);

        assertNotNull(result);
        assertEquals(4, result.getScore());
        assertEquals(participant.getId(), result.getRater().getId());
        assertEquals(organizer.getId(), result.getOrganizer().getId());
        Mockito.verify(ratingRepository, Mockito.times(1)).save(Mockito.any(Rating.class));
    }

    @Test
    public void createRating_invalidScore_throwsBadRequest() {
        Long eventId = 10L;
        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(eventId, participant, 6));
        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(eventId, participant, 0));
        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(eventId, participant, null));
    }

    @Test
    public void createRating_eventNotFound_throwsNotFound() {
        Long missingId = 99L;
        Mockito.when(eventRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(missingId, participant, 3));
    }

    @Test
    public void createRating_eventNotEnded_throwsBadRequest() {
        Long eventId = 10L;
        event.setEndTime(LocalDateTime.now().plusHours(2));
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(eventId, participant, 3));
    }

    @Test
    public void createRating_creatorRatesSelf_throwsForbidden() {
        Long eventId = 10L;
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(eventId, organizer, 3));
    }

    @Test
    public void createRating_nonParticipant_throwsForbidden() {
        Long eventId = 10L;
        User outsider = new User();
        outsider.setId(3L);
        outsider.setUsername("outsider");

        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(eventId, outsider, 3));
    }

    @Test
    public void createRating_duplicateRating_throwsConflict() {
        Long eventId = 10L;
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        Mockito.when(ratingRepository.existsByRaterAndEvent(participant, event)).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> ratingService.createRating(eventId, participant, 4));
    }

    @Test
    public void getMyRating_existingRating_returned() {
        Long eventId = 10L;
        Rating rating = new Rating();
        rating.setId(1L);
        rating.setScore(5);

        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        Mockito.when(ratingRepository.findByRaterAndEvent(participant, event)).thenReturn(rating);

        Rating result = ratingService.getMyRating(participant, eventId);

        assertNotNull(result);
        assertEquals(5, result.getScore());
    }

    @Test
    public void getMyRating_noRating_returnsNull() {
        Long eventId = 10L;
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        Mockito.when(ratingRepository.findByRaterAndEvent(participant, event)).thenReturn(null);

        Rating result = ratingService.getMyRating(participant, eventId);

        assertNull(result);
    }

    @Test
    public void getMyRating_eventNotFound_throwsNotFound() {
        Long missingId = 99L;
        Mockito.when(eventRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> ratingService.getMyRating(participant, missingId));
    }

    @Test
    public void getAverageRating_withRatings_returnsCorrectAverage() {
        Rating r1 = new Rating(); r1.setScore(4);
        Rating r2 = new Rating(); r2.setScore(2);

        Mockito.when(ratingRepository.findByOrganizer(organizer)).thenReturn(List.of(r1, r2));

        double avg = ratingService.getAverageRating(organizer);

        assertEquals(3.0, avg, 0.001);
    }

    @Test
    public void getAverageRating_noRatings_returnsZero() {
        Mockito.when(ratingRepository.findByOrganizer(organizer)).thenReturn(new ArrayList<>());

        double avg = ratingService.getAverageRating(organizer);

        assertEquals(0.0, avg, 0.001);
    }

    @Test
    public void getRatingCount_withRatings_returnsCount() {
        Rating r1 = new Rating();
        Rating r2 = new Rating();
        Rating r3 = new Rating();

        Mockito.when(ratingRepository.findByOrganizer(organizer)).thenReturn(List.of(r1, r2, r3));

        int count = ratingService.getRatingCount(organizer);

        assertEquals(3, count);
    }

    @Test
    public void getRatingCount_noRatings_returnsZero() {
        Mockito.when(ratingRepository.findByOrganizer(organizer)).thenReturn(new ArrayList<>());

        int count = ratingService.getRatingCount(organizer);

        assertEquals(0, count);
    }
}
