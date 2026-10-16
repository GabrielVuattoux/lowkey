package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PublicEventGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {
	@Test
	public void testCreateUser_fromUserPostDTO_toUser_success() {
		// create UserPostDTO
		UserPostDTO userPostDTO = new UserPostDTO();
		// userPostDTO.setName("name");
		userPostDTO.setUsername("username");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		// assertEquals(userPostDTO.getName(), user.getName());
		assertEquals(userPostDTO.getUsername(), user.getUsername());
	}

	@Test
	public void testGetUser_fromUser_toUserGetDTO_success() {
		// create User
		User user = new User();
		// user.setName("Firstname Lastname");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check content
		assertEquals(user.getId(), userGetDTO.getId());
		// assertEquals(user.getName(), userGetDTO.getName());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
	}

	@Test
	public void testGetPublicEvent_fromEvent_toPublicEventGetDTO_success() {
		User creator = new User();
		creator.setId(1L);
		User participant = new User();
		participant.setId(2L);

		Event event = new Event();
		event.setId(7L);
		event.setTitle("Board games night");
		event.setDescription("Bring your favourite game");
		event.setCategory(EventCategory.SOCIAL);
		event.setStartTime(LocalDateTime.of(2026, 10, 1, 19, 0));
		event.setEndTime(LocalDateTime.of(2026, 10, 1, 22, 0));
		event.setLatitude(47.3764);
		event.setLongitude(8.5480);
		event.setCreator(creator);
		event.setParticipants(List.of(creator, participant));
		event.setInviteCode("SECRET12");

		PublicEventGetDTO dto = DTOMapper.INSTANCE.convertEntityToPublicEventGetDTO(event);

		assertEquals(event.getId(), dto.getId());
		assertEquals(event.getTitle(), dto.getTitle());
		assertEquals(event.getDescription(), dto.getDescription());
		assertEquals(event.getCategory(), dto.getCategory());
		assertEquals(event.getStartTime(), dto.getStartTime());
		assertEquals(event.getEndTime(), dto.getEndTime());
		assertEquals(event.getLatitude(), dto.getLatitude());
		assertEquals(event.getLongitude(), dto.getLongitude());
		assertEquals(2, dto.getParticipantCount());
	}
}
