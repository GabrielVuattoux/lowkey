package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.EventService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Demo data for local development, only with the "dev" profile:
 * ./gradlew bootRun --args='--spring.profiles.active=dev'
 *
 * Seeds public events around Zurich, plus a private, a cancelled and a far-away event
 * that must never show up in the mobile app's "around you" list.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;

    DevDataSeeder(@Qualifier("userRepository") UserRepository userRepository,
                  @Qualifier("eventRepository") EventRepository eventRepository,
                  EventService eventService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
    }

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            return;
        }
        User host = demoUser("lowkey-demo");
        List<User> friends = List.of(demoUser("demo-friend-1"), demoUser("demo-friend-2"));
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        event(host, "Sunset volleyball", "Two players short, all levels welcome.",
                EventCategory.SPORTS, 47.3563, 8.5360, now.plusHours(2), 2, false, friends);
        event(host, "Open mic at the Letten", "Bring your guitar or just come and listen.",
                EventCategory.MUSIC, 47.3868, 8.5360, now.plusHours(4), 3, false, friends.subList(0, 1));
        event(host, "Picnic at Lindenhof", "Everyone brings something to share.",
                EventCategory.FOOD, 47.3730, 8.5410, now.plusDays(1).plusHours(1), 2, false, List.of());
        event(host, "Sketching at the Kunsthaus", "Pencils provided, no experience needed.",
                EventCategory.ART, 47.3703, 8.5483, now.plusDays(1).plusHours(3), 2, false, friends);
        event(host, "Board games night", "Catan, Codenames and whatever you bring.",
                EventCategory.SOCIAL, 47.3764, 8.5480, now.plusHours(6), 3, false, List.of());
        event(host, "Uetliberg sunrise hike", "Meet at the Uetliberg train station.",
                EventCategory.OUTDOOR, 47.3496, 8.4917, now.plusDays(2).withHour(6), 3, false, friends.subList(1, 2));

        // Never returned by /public/events: private, cancelled, or outside the Zurich cells
        event(host, "Birthday at Anna's", "Friends only.",
                EventCategory.PARTY, 47.3667, 8.5500, now.plusHours(5), 4, true, friends);
        Event cancelled = event(host, "Cancelled jam session", "Called off.",
                EventCategory.MUSIC, 47.3780, 8.5400, now.plusHours(3), 2, false, List.of());
        cancelled.setCancelledAt(LocalDateTime.now());
        eventRepository.save(cancelled);
        event(host, "Aare swim in Bern", "Too far from Zurich to show up there.",
                EventCategory.OUTDOOR, 46.9480, 7.4474, now.plusDays(1), 2, false, List.of());

        log.info("dev profile: seeded {} demo events", eventRepository.count());
    }

    private User demoUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.invalid");
        // Random secrets: nobody can log in as a demo user
        user.setPassword(UUID.randomUUID().toString());
        user.setToken(UUID.randomUUID().toString());
        user.setStatus(UserStatus.OFFLINE);
        return userRepository.save(user);
    }

    private Event event(User host, String title, String description, EventCategory category,
                        double latitude, double longitude, LocalDateTime start, int hours,
                        boolean isPrivate, List<User> participants) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setCategory(category);
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        event.setStartTime(start);
        event.setEndTime(start.plusHours(hours));
        event.setIsPrivate(isPrivate);
        event.setParticipants(new ArrayList<>(participants));
        return eventService.createEvent(event, host);
    }
}
