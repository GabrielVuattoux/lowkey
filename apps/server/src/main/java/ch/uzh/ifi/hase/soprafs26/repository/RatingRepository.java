package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Rating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("ratingRepository")
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByOrganizer(User organizer);
    Rating findByRaterAndEvent(User rater, Event event);
    boolean existsByRaterAndEvent(User rater, Event event);
}