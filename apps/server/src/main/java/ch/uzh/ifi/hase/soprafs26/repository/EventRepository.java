package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.Event;

import java.time.LocalDateTime;
import java.util.List;

@Repository("eventRepository")
public interface EventRepository extends JpaRepository<Event, Long> {
	Event findById(long id);
	Event findByInviteCode(String inviteCode);

	@Query("SELECT e FROM Event e JOIN e.participants p WHERE p.id = :userId")
	List<Event> findByParticipantId(@Param("userId") Long userId);

	List<Event> findByCancelledAtIsNotNullAndCancelledAtBefore(LocalDateTime cutoff);
}
