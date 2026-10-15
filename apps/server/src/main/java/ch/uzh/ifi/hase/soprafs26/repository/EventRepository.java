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

	// Participation check without loading the lazy participant list (used outside transactions)
	boolean existsByIdAndParticipants_Id(Long eventId, Long userId);

	// Public, not cancelled and not yet ended events inside a bounding box (one geohash cell)
	@Query("SELECT e FROM Event e WHERE e.isPrivate = false AND e.cancelledAt IS NULL AND e.endTime > :now"
			+ " AND e.latitude BETWEEN :minLat AND :maxLat AND e.longitude BETWEEN :minLng AND :maxLng")
	List<Event> findPublicUpcomingInBox(@Param("now") LocalDateTime now,
			@Param("minLat") double minLat, @Param("maxLat") double maxLat,
			@Param("minLng") double minLng, @Param("maxLng") double maxLng);
}
