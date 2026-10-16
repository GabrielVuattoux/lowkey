package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.RatingService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.service.EventService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private EventService eventService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RatingService ratingService;

	private User requester;

	// Every request below is sent as user 1, unless a test stubs findByToken itself
	@BeforeEach
	public void setupAuthentication() {
		requester = new User();
		requester.setId(1L);
		requester.setUsername("requester");
		given(userRepository.findByToken(anyString())).willReturn(requester);
	}

	@Test
	public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
		// given
		User user = new User();
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);

		List<User> allUsers = Collections.singletonList(user);

		// this mocks the UserService -> we define above what the userService should
		// return when getUsers() is called
		given(userService.getUsers()).willReturn(allUsers);

		// when
		MockHttpServletRequestBuilder getRequest = get("/users").header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON);

		// then
		mockMvc.perform(getRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].username", is(user.getUsername())))
				.andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
	}

	@Test
	public void givenUser_whenGetUser_thenReturnJson() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setStatus(UserStatus.ONLINE);

		given(userService.getUser(user.getId())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder getRequest = get("/users/{id}", user.getId())
				.header("Authorization", "Bearer token")
				.contentType(MediaType.APPLICATION_JSON);

		mockMvc.perform(getRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	@Test
	public void createUser_validInput_userCreated() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testUsername");

		given(userService.createUser(Mockito.any())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	@Test
	public void loginUser_validInput_userLoggedIn() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("test token");

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testUsername");

		given(userService.loginUser(Mockito.any())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/users/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())))
				.andExpect(jsonPath("$.token", is(user.getToken())));
		
	}

	@Test
	public void updateUser_validInput_userUpdated() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);

		User userUpdated = new User();
		userUpdated.setId(1L);
		userUpdated.setUsername("updatedUsername");
		userUpdated.setToken("1");
		userUpdated.setStatus(UserStatus.ONLINE);

		UserPutDTO userPutDTO = new UserPutDTO();
		// userPostDTO.setName("Test User");
		userPutDTO.setUsername("updatedUsername");

		given(userService.updateUser(Mockito.anyLong(), Mockito.any())).willReturn(userUpdated);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = put("/users/{id}", user.getId())
				.header("Authorization", "Bearer token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPutDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(userUpdated.getId().intValue())))
				.andExpect(jsonPath("$.username", is(userUpdated.getUsername())))
				.andExpect(jsonPath("$.status", is(userUpdated.getStatus().toString())));
	
	}

	@Test
	public void followUser_validInput_userFollowed() throws Exception {
		// given
		User follower = new User();
		follower.setId(1L);
		follower.setUsername("followerUsername");
		follower.setToken("1");
		follower.setStatus(UserStatus.ONLINE);
		follower.setFollowing(new HashSet<>());

		User targetUser = new User();
		targetUser.setId(2L);
		targetUser.setUsername("targetUsername");
		targetUser.setToken("2");
		targetUser.setStatus(UserStatus.ONLINE);
		targetUser.setFollowing(new HashSet<>());

		follower.getFollowing().add(targetUser);

		
		given(userRepository.findByToken(anyString())).willReturn(follower);

		given(userService.followUser(follower.getId(), targetUser.getId()))
            .willReturn(follower);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/users/{targetUserId}/follow", targetUser.getId())
				.header("Authorization", "Bearer 1")
				.contentType(MediaType.APPLICATION_JSON);

		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(follower.getId().intValue())))
				.andExpect(jsonPath("$.username", is(follower.getUsername())))
				.andExpect(jsonPath("$.status", is(follower.getStatus().toString())))
				.andExpect(jsonPath("$.followingIds[0]", is(targetUser.getId().intValue())));
	
	}

	@Test
	public void followingUsers_getFollowingUsers_followingUsersReturned() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");

		User followedUser = new User();
		followedUser.setId(2L);
		followedUser.setUsername("followedUsername");


		user.setFollowing(new HashSet<>(Set.of(followedUser)));
		
		given(userRepository.findByToken(anyString())).willReturn(user);
		given(userService.getFollowingUsers(user.getId())).willReturn(user.getFollowing());

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder getRequest = get("/users/following")
				.header("Authorization", "Bearer token")
				.contentType(MediaType.APPLICATION_JSON);
		
		mockMvc.perform(getRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(followedUser.getId().intValue())))
				.andExpect(jsonPath("$[0].username", is(followedUser.getUsername())));
	}

	@Test
	public void getUser_notFound_returns404() throws Exception {
		// given
		given(userService.getUser(99L))
				.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

		// when + then
		mockMvc.perform(get("/users/99").header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void createUser_duplicateUsername_returns400() throws Exception {
		// given
		given(userService.createUser(Mockito.any()))
				.willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is not unique"));

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("existingUsername");

		// when + then
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJsonString(userPostDTO)))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void loginUser_invalidCredentials_returns400() throws Exception {
		// given
		given(userService.loginUser(Mockito.any()))
				.willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "wrong credentials"));

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("someUser");

		// when + then
		mockMvc.perform(post("/users/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJsonString(userPostDTO)))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void getUserEvents_validUser_returnsEventList() throws Exception {
		// given
		User creator = new User();
		creator.setId(1L);
		creator.setUsername("testUsername");

		Event event = new Event();
		event.setId(1L);
		event.setTitle("Test Event");
		event.setDescription("desc");
		event.setIsPrivate(false);
		event.setLatitude(47.0);
		event.setLongitude(8.0);
		event.setStartTime(LocalDateTime.of(2026, 6, 1, 10, 0));
		event.setEndTime(LocalDateTime.of(2026, 6, 1, 12, 0));
		event.setCategory(EventCategory.SPORTS);
		event.setCreator(creator);
		event.setInviteCode("ABC123");

		given(eventService.getEventsByUserId(1L)).willReturn(List.of(event));

		// when + then
		mockMvc.perform(get("/users/1/events").header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(1)));
	}

	@Test
	public void getFollowers_validUser_returnsFollowerList() throws Exception {
		// given
		User follower = new User();
		follower.setId(2L);
		follower.setUsername("followerUser");
		follower.setStatus(UserStatus.ONLINE);

		given(userService.getFollowers(1L)).willReturn(List.of(follower));

		// when + then
		mockMvc.perform(get("/users/1/followers").header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(2)))
				.andExpect(jsonPath("$[0].username", is("followerUser")));
	}

	@Test
	public void getFollowing_byUserId_returnsFollowingList() throws Exception {
		// given
		User followed = new User();
		followed.setId(3L);
		followed.setUsername("followedUser");
		followed.setStatus(UserStatus.ONLINE);

		given(userService.getFollowingUsers(1L)).willReturn(new HashSet<>(Set.of(followed)));

		// when + then
		mockMvc.perform(get("/users/1/following").header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(3)))
				.andExpect(jsonPath("$[0].username", is("followedUser")));
	}

	@Test
	public void unfollowUser_validInput_returnsUpdatedUser() throws Exception {
		// given
		User follower = new User();
		follower.setId(1L);
		follower.setUsername("followerUsername");
		follower.setToken("1");
		follower.setStatus(UserStatus.ONLINE);
		follower.setFollowing(new HashSet<>());

		given(userRepository.findByToken(anyString())).willReturn(follower);
		given(userService.unfollowUser(follower.getId(), 2L)).willReturn(follower);

		// when + then
		mockMvc.perform(delete("/users/2/follow")
						.header("Authorization", "Bearer 1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(follower.getId().intValue())))
				.andExpect(jsonPath("$.username", is(follower.getUsername())));
	}

	@Test
	public void getUsers_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void getUser_otherUser_hidesEmail() throws Exception {
		User other = new User();
		other.setId(2L);
		other.setUsername("other");
		other.setEmail("other@example.com");
		given(userService.getUser(2L)).willReturn(other);

		mockMvc.perform(get("/users/2").header("Authorization", "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", is("other")))
				.andExpect(jsonPath("$.email").doesNotExist());
	}

	@Test
	public void getUser_self_includesEmail() throws Exception {
		requester.setEmail("me@example.com");
		given(userService.getUser(1L)).willReturn(requester);

		mockMvc.perform(get("/users/1").header("Authorization", "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("me@example.com")));
	}

	@Test
	public void updateUser_otherUser_returns403() throws Exception {
		UserPutDTO userPutDTO = new UserPutDTO();
		userPutDTO.setPassword("hijacked");

		mockMvc.perform(put("/users/2")
						.header("Authorization", "Bearer token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJsonString(userPutDTO)))
				.andExpect(status().isForbidden());

		verify(userService, never()).updateUser(Mockito.anyLong(), Mockito.any());
	}

	@Test
	public void updateUser_withoutToken_returns401() throws Exception {
		mockMvc.perform(put("/users/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(asJsonString(new UserPutDTO())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void getUserEvents_hidesOthersPrivateEventsAndInviteCodes() throws Exception {
		User owner = new User();
		owner.setId(2L);

		Event publicEvent = new Event();
		publicEvent.setId(1L);
		publicEvent.setIsPrivate(false);
		publicEvent.setCreator(owner);
		publicEvent.setParticipants(List.of(owner));
		publicEvent.setInviteCode("PUBLIC01");

		Event privateEvent = new Event();
		privateEvent.setId(2L);
		privateEvent.setIsPrivate(true);
		privateEvent.setCreator(owner);
		privateEvent.setParticipants(List.of(owner));
		privateEvent.setInviteCode("SECRET01");

		given(eventService.getEventsByUserId(2L)).willReturn(List.of(publicEvent, privateEvent));

		mockMvc.perform(get("/users/2/events").header("Authorization", "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[0].inviteCode").doesNotExist());
	}

	@Test
	public void getUserEvents_participant_seesPrivateEventAndInviteCode() throws Exception {
		User owner = new User();
		owner.setId(2L);

		Event privateEvent = new Event();
		privateEvent.setId(2L);
		privateEvent.setIsPrivate(true);
		privateEvent.setCreator(owner);
		privateEvent.setParticipants(List.of(owner, requester));
		privateEvent.setInviteCode("SECRET01");

		given(eventService.getEventsByUserId(2L)).willReturn(List.of(privateEvent));

		mockMvc.perform(get("/users/2/events").header("Authorization", "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].inviteCode", is("SECRET01")));
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