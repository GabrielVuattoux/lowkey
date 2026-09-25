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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
public class EventServiceIntegrationTest {

    @Qualifier("eventRepository")
    @Autowired
    private EventRepository eventRepository;

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventService eventService;

    // Zurich HB, used as the search origin
    private static final double ZURICH_LAT = 47.3769;
    private static final double ZURICH_LON = 8.5417;

    // Bern is outside of a 20 km radius
    private static final double BERN_LAT = 46.9481;
    private static final double BERN_LON = 7.4474;

    private User creator;
    private User searcher;

    @BeforeEach
    public void setup() {
        // clean db so tests dont influence each other
        eventRepository.deleteAll();
        userRepository.deleteAll();

        creator = new User();
        creator.setUsername("creator");
        creator.setEmail("creator@example.com");
        creator.setPassword("pw");
        creator.setToken("t-creator");
        creator.setStatus(UserStatus.ONLINE);
        creator = userRepository.save(creator);

        searcher = new User();
        searcher.setUsername("searcher");
        searcher.setEmail("searcher@example.com");
        searcher.setPassword("pw");
        searcher.setToken("t-searcher");
        searcher.setStatus(UserStatus.ONLINE);
        searcher = userRepository.save(searcher);
    }

    @Test
    public void createEvent_validInput_success() {
        // given
        Event input = new Event();
        input.setTitle("Yoga in the Park");
        input.setDescription("desc");
        input.setIsPrivate(false);
        input.setLatitude(ZURICH_LAT);
        input.setLongitude(ZURICH_LON);
        input.setStartTime(LocalDateTime.now().plusHours(2));
        input.setEndTime(LocalDateTime.now().plusHours(4));
        input.setCategory(EventCategory.SPORTS);

        // when
        Event saved = eventService.createEvent(input, creator);

        // then -> creator added to participants, invite code generated, persisted
        assertNotNull(saved.getId());
        assertEquals(creator.getId(), saved.getCreator().getId());
        assertTrue(saved.getParticipants().stream()
                .anyMatch(u -> u.getId().equals(creator.getId())));
        assertNotNull(saved.getInviteCode());
        assertFalse(saved.getInviteCode().isBlank());

        // check its really in the repo
        Event fromDb = eventRepository.findById((long) saved.getId());
        assertNotNull(fromDb);
        assertEquals(saved.getInviteCode(), fromDb.getInviteCode());
    }

    @Test
    public void createEvent_endBeforeStart_throwsException() {
        // given -> endTime is before startTime
        Event input = new Event();
        input.setTitle("Broken Event");
        input.setDescription("desc");
        input.setIsPrivate(false);
        input.setLatitude(ZURICH_LAT);
        input.setLongitude(ZURICH_LON);
        input.setStartTime(LocalDateTime.now().plusHours(3));
        input.setEndTime(LocalDateTime.now().plusHours(1));
        input.setCategory(EventCategory.SPORTS);

        // then
        assertThrows(ResponseStatusException.class,
                () -> eventService.createEvent(input, creator));

        // nothing should have been stored
        assertTrue(eventRepository.findAll().isEmpty());
    }

    @Test
    public void getEventsInRadius_filtersDistanceTimePrivacyAndCategory() {
        // A: nearby, public, future, SPORTS -> should be found
        createAndSave("Nearby Public Sports", ZURICH_LAT, ZURICH_LON, false,
                EventCategory.SPORTS,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3), null);

        // B: far away in Bern -> filtered out by distance
        createAndSave("Far Public Sports", BERN_LAT, BERN_LON, false,
                EventCategory.SPORTS,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3), null);

        // D: private event, searcher is not a participant -> filtered out
        createAndSave("Hidden Private", ZURICH_LAT, ZURICH_LON, true,
                EventCategory.SPORTS,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3), null);

        // E: private event, searcher IS a participant -> should be found
        List<User> withSearcher = new ArrayList<>();
        withSearcher.add(searcher);
        createAndSave("Visible Private", ZURICH_LAT, ZURICH_LON, true,
                EventCategory.SPORTS,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3), withSearcher);

        // F: nearby and future but wrong category
        createAndSave("Wrong Category", ZURICH_LAT, ZURICH_LON, false,
                EventCategory.SOCIAL,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3), null);

        // when -> searcher queries from Zurich, 20km radius, only SPORTS
        List<Event> result = eventService.getEventsInRadius(
                ZURICH_LAT, ZURICH_LON, 20.0, searcher,
                Set.of(EventCategory.SPORTS), false);

        // then -> only A and E should survive all four filters
        assertEquals(2, result.size());
        List<String> titles = result.stream().map(Event::getTitle).toList();
        assertTrue(titles.contains("Nearby Public Sports"));
        assertTrue(titles.contains("Visible Private"));

        // without the category filter the SOCIAL event should reappear
        List<Event> noCategory = eventService.getEventsInRadius(
                ZURICH_LAT, ZURICH_LON, 20.0, searcher, null, false);
        assertTrue(noCategory.stream()
                .anyMatch(e -> e.getTitle().equals("Wrong Category")));
    }

    // small helper so we dont repeat the same setup over and over
    private void createAndSave(String title, double lat, double lon,
                               boolean isPrivate, EventCategory category,
                               LocalDateTime start, LocalDateTime end,
                               List<User> extraParticipants) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription("desc");
        e.setIsPrivate(isPrivate);
        e.setLatitude(lat);
        e.setLongitude(lon);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setCategory(category);
        if (extraParticipants != null) {
            e.setParticipants(extraParticipants);
        }
        eventService.createEvent(e, creator);
    }
}