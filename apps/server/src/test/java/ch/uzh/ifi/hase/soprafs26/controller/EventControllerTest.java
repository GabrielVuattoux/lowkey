package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.service.EventService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.service.ParticipantService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
//import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventPostDTO;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Optional;


/**
 * EntryControllerTest
 * This is a WebMvcTest which allows to test the EntryController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the EntryController works.
 */
@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EventService eventService;

	@MockitoBean
	private EventRepository eventRepository;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private ParticipantService participantService;

    @BeforeEach
    public void setupAuthentication() {
        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        given(userRepository.findByToken(anyString())).willReturn(authenticatedUser);
    }

	@Test
	public void createEvent_whenValidInput_thenReturns201() throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

		LocalDateTime startTime = LocalDateTime.now().plusDays(7).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
		LocalDateTime endTime = LocalDateTime.now().plusDays(8).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
		
		User creator = new User();
		creator.setId(1L);
		creator.setUsername("alice");

		Event event = new Event();
		event.setTitle("Test Event");
		event.setDescription("A description");
		event.setIsPrivate(false);
		event.setLatitude(47.3769);
		event.setLongitude(8.5417);
		event.setStartTime(startTime);
		event.setEndTime(endTime);
		event.setCategory(EventCategory.SPORTS);
		event.setCreator(creator);

		EventPostDTO eventPostDTO = new EventPostDTO();
		eventPostDTO.setTitle("Test Event");
		eventPostDTO.setDescription("A description");
		eventPostDTO.setIsPrivate(false);
		eventPostDTO.setLatitude(47.3769);
		eventPostDTO.setLongitude(8.5417);
		eventPostDTO.setStartTime(startTime);
		eventPostDTO.setEndTime(endTime);
		eventPostDTO.setCategory(EventCategory.SPORTS);

		// Mockito.doNothing().when(userService).validateToken(anyString());
		given(eventService.createEvent(Mockito.any(Event.class), Mockito.any(User.class)))
				.willReturn(event);

		MockHttpServletRequestBuilder postRequest = post("/events")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token")
				.content(asJsonString(eventPostDTO));


		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.title", is(event.getTitle())))
				.andExpect(jsonPath("$.description", is(event.getDescription())))
				.andExpect(jsonPath("$.isPrivate", is(event.getIsPrivate())))
				.andExpect(jsonPath("$.latitude", is(event.getLatitude())))
				.andExpect(jsonPath("$.longitude", is(event.getLongitude())))
				.andExpect(jsonPath("$.startTime", is(event.getStartTime().format(formatter))))
				.andExpect(jsonPath("$.endTime", is(event.getEndTime().format(formatter))))
				.andExpect(jsonPath("$.category", is(event.getCategory().toString())));
	}

	@Test
	public void givenEvents_whenGetEvents_thenReturnJsonArray() throws Exception {
		// given
		Event event = new Event();
		event.setTitle("Test Event");
		event.setDescription("Test Event");
		event.setIsPrivate(false);
		event.setLatitude(47.3769);
		event.setLongitude(8.5417);

		List<Event> allEvents = Collections.singletonList(event);

		// Mockito.doNothing().when(userService).validateToken(anyString());

		// this mocks the EventService -> we define above what the eventService should
		// return when getEventsInRadius() is called
		given(eventService.getEventsInRadius(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any(User.class), Mockito.<Set<EventCategory>>any(), Mockito.anyBoolean()))
			.willReturn(allEvents);

		// when
		MockHttpServletRequestBuilder getRequest = get("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .param("latitude", "47.3769")
                .param("longitude", "8.5417")
                .param("radius", "20");

		// then
		mockMvc.perform(getRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title", is(event.getTitle())))
				.andExpect(jsonPath("$[0].description", is(event.getDescription())))
				.andExpect(jsonPath("$[0].isPrivate", is(event.getIsPrivate())))
				.andExpect(jsonPath("$[0].latitude", is(event.getLatitude())));
	}

	@Test
	public void getEventById_whenExists_returnsEvent() throws Exception {
		User creator = new User();
		creator.setUsername("alice");

		Event event = new Event();
		event.setTitle("Test Event");
		event.setDescription("A description");
		event.setIsPrivate(false);
		event.setLatitude(47.3769);
		event.setLongitude(8.5417);
		event.setCreator(creator);
		event.setParticipants(List.of(creator));

		// Mockito.doNothing().when(userService).validateToken(anyString());
		given(eventService.getEventById(1L)).willReturn(event);

		MockHttpServletRequestBuilder getRequest = get("/events/1")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token");

		mockMvc.perform(getRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title", is(event.getTitle())))
				.andExpect(jsonPath("$.description", is(event.getDescription())))
				.andExpect(jsonPath("$.creatorUsername", is("alice")))
				.andExpect(jsonPath("$.participantCount", is(1)));
	}

	@Test
	public void getEventById_whenNotFound_returns404() throws Exception {
		// Mockito.doNothing().when(userService).validateToken(anyString());
		given(eventService.getEventById(99L))
				.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

		MockHttpServletRequestBuilder getRequest = get("/events/99")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token");

		mockMvc.perform(getRequest).andExpect(status().isNotFound());
	}

	// POST /events/{eventId}/participants
	@Test
	public void joinEventById_whenEventPublic_returns201() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");

		Event event = new Event();
		event.setId(1L);
		event.setTitle("Public Event");
		event.setIsPrivate(false);
		event.setParticipants(List.of());

		// Mockito.doNothing().when(userService).validateToken(anyString());

		given(participantService.joinEventById(1L, 1L)).willReturn(event);

		MockHttpServletRequestBuilder postRequest = post("/events/1/participants")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token")
				.content("{\"userId\": 1}");

		mockMvc.perform(postRequest).andExpect(status().isCreated())
				.andExpect(jsonPath("$.title", is(event.getTitle())))
				.andExpect(jsonPath("$.isPrivate", is(event.getIsPrivate())))
				.andExpect(jsonPath("$.participantCount", is(0)));
	}


	@Test 
	public void joinEventById_whenEventIsPrivate_returns403() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");

		Event event = new Event();
		event.setId(1L);
		event.setTitle("Private Event");
		event.setIsPrivate(true);
		event.setParticipants(List.of());

		// Mockito.doNothing().when(userService).validateToken(anyString());

		given(participantService.joinEventById(1L, 1L))
			.willThrow(new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Cannot join a private event without an invite code"
			));

		MockHttpServletRequestBuilder postRequest = post("/events/1/participants")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token")
				.content("{\"userId\": 1}");

		mockMvc.perform(postRequest).andExpect(status().isForbidden());
	}

	@Test
	public void joinEventByinviteCode_whenEventIsPrivate_returns201() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");

		Event event = new Event();
		event.setId(1L);
		event.setTitle("Private Event");
		event.setIsPrivate(true);
		event.setParticipants(List.of());

		// Mockito.doNothing().when(userService).validateToken(anyString());

		given(participantService.joinEventByInviteCode("invite-code", 1L)).willReturn(event);

		MockHttpServletRequestBuilder postRequest = post("/events/participants")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token")
				.content("{\"inviteCode\": \"invite-code\", \"userId\": 1}");

		mockMvc.perform(postRequest).andExpect(status().isCreated())
				.andExpect(jsonPath("$.title", is(event.getTitle())))
				.andExpect(jsonPath("$.isPrivate", is(event.getIsPrivate())))
				.andExpect(jsonPath("$.participantCount", is(0)));
	}

	@Test
	public void updateEvent_whenUserIsCreator_returns204() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");

		Event event = new Event();
		event.setId(1L);
		event.setTitle("Test Event");
		event.setIsPrivate(false);
		event.setCreator(user);

		//Mockito.doNothing().when(userService).validateToken(anyString());
		given(eventService.getEventById(1L)).willReturn(event);

		String updatedEventJson = "{\"title\": \"Updated Event\", \"description\": \"Updated Description\"}";

		MockHttpServletRequestBuilder putRequest = put("/events/1")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token")
				.content(updatedEventJson);

		mockMvc.perform(putRequest).andExpect(status().isNoContent());
	}

	@Test
	public void deleteEventParticipant_whenUserIsParticipant_returns204() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");

		Event event = new Event();
		event.setId(1L);
		event.setTitle("Test Event");
		event.setIsPrivate(false);
		event.setParticipants(List.of(user));

		// Mockito.doNothing().when(userService).validateTokenToUser(anyString(), Mockito.anyLong());
		given(eventService.getEventById(1L)).willReturn(event);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		MockHttpServletRequestBuilder deleteRequest = delete("/events/1/participants/1")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token");

		mockMvc.perform(deleteRequest).andExpect(status().isNoContent())
				.andExpect(jsonPath("$").doesNotExist());
	}

	@Test
	public void deleteEvent_whenUserIsCreator_returns204() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");

		Event event = new Event();
		event.setId(1L);
		event.setTitle("Test Event");
		event.setIsPrivate(false);
		event.setCreator(user);

		// Mockito.doNothing().when(userService).validateToken(anyString());
		given(eventService.getEventById(1L)).willReturn(event);

		MockHttpServletRequestBuilder deleteRequest = delete("/events/1")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer test-token");

		mockMvc.perform(deleteRequest).andExpect(status().isNoContent())
				.andExpect(jsonPath("$").doesNotExist());

		assertNull(eventRepository.findById(1L));
	}

	@Test
	public void deleteEvent_whenUserIsNotCreator_returns403() throws Exception {
		// given
		Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete this event"))
			.when(eventService).deleteEvent(eq(1L), Mockito.any(User.class));

		MockHttpServletRequestBuilder request = delete("/events/1")
			.header("Authorization", "Bearer valid-token")
			.contentType(MediaType.APPLICATION_JSON);

		// when + then
		mockMvc.perform(request)
			.andExpect(status().isForbidden());
	}

	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 *
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}