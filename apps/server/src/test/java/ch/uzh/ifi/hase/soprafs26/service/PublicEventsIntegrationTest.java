package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
public class PublicEventsIntegrationTest {

    // Precision 5 geohash cells, as the mobile app computes them
    private static final String ZURICH_HB_CELL = "u0qjd";
    private static final String BERN_CELL = "u0m71";

    private static final double ZURICH_LAT = 47.3769;
    private static final double ZURICH_LON = 8.5417;
    private static final double BERN_LAT = 46.9481;
    private static final double BERN_LON = 7.4474;

    @Qualifier("eventRepository")
    @Autowired
    private EventRepository eventRepository;

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventService eventService;

    private User creator;

    @BeforeEach
    public void setup() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        creator = new User();
        creator.setUsername("creator");
        creator.setEmail("creator@example.com");
        creator.setPassword("pw");
        creator.setToken("t-creator");
        creator.setStatus(UserStatus.ONLINE);
        creator = userRepository.save(creator);
    }

    private Event saveEvent(String title, double latitude, double longitude, boolean isPrivate,
                            LocalDateTime start, LocalDateTime end) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("desc");
        event.setCategory(EventCategory.SOCIAL);
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        event.setIsPrivate(isPrivate);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setCreator(creator);
        event.setParticipants(new ArrayList<>(List.of(creator)));
        event.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        return eventRepository.save(event);
    }

    private Event savePublicUpcoming(String title, double latitude, double longitude, int startsInHours) {
        LocalDateTime start = LocalDateTime.now().plusHours(startsInHours);
        return saveEvent(title, latitude, longitude, false, start, start.plusHours(2));
    }

    @Test
    public void getPublicEventsInCells_returnsOnlyPublicUpcomingEventsOfTheCell() {
        Event visible = savePublicUpcoming("Public in Zurich", ZURICH_LAT, ZURICH_LON, 1);
        saveEvent("Private in Zurich", ZURICH_LAT, ZURICH_LON, true,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3));
        Event cancelled = savePublicUpcoming("Cancelled in Zurich", ZURICH_LAT, ZURICH_LON, 1);
        cancelled.setCancelledAt(LocalDateTime.now());
        eventRepository.save(cancelled);
        saveEvent("Ended in Zurich", ZURICH_LAT, ZURICH_LON, false,
                LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(1));
        savePublicUpcoming("Public in Bern", BERN_LAT, BERN_LON, 1);

        List<Event> result = eventService.getPublicEventsInCells(List.of(ZURICH_HB_CELL));

        assertEquals(1, result.size());
        assertEquals(visible.getId(), result.get(0).getId());
    }

    @Test
    public void getPublicEventsInCells_overlappingCells_returnsEachEventOnceSortedByStart() {
        Event later = savePublicUpcoming("Later", ZURICH_LAT, ZURICH_LON, 5);
        Event sooner = savePublicUpcoming("Sooner", ZURICH_LAT, ZURICH_LON, 1);

        // "u0qj" (precision 4) contains "u0qjd" (precision 5)
        List<Event> result = eventService.getPublicEventsInCells(List.of(ZURICH_HB_CELL, "u0qj"));

        assertEquals(List.of(sooner.getId(), later.getId()), result.stream().map(Event::getId).toList());
    }

    @Test
    public void getPublicEventsInCells_otherCell_returnsOnlyThatCellsEvents() {
        savePublicUpcoming("Public in Zurich", ZURICH_LAT, ZURICH_LON, 1);
        Event bern = savePublicUpcoming("Public in Bern", BERN_LAT, BERN_LON, 1);

        List<Event> result = eventService.getPublicEventsInCells(List.of(BERN_CELL));

        assertEquals(1, result.size());
        assertEquals(bern.getId(), result.get(0).getId());
    }

    @Test
    public void getPublicEventsInCells_cellTooPrecise_throwsBadRequest() {
        // Precision 6 (about 1.2 x 0.6 km) would reveal too much about the user's position
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getPublicEventsInCells(List.of("u0qjd1")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void getPublicEventsInCells_invalidCharacters_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getPublicEventsInCells(List.of("u0qja")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void getPublicEventsInCells_tooManyCells_throwsBadRequest() {
        List<String> cells = Collections.nCopies(10, ZURICH_HB_CELL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getPublicEventsInCells(cells));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void getPublicEventsInCells_noCells_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getPublicEventsInCells(List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
