package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(1L);
		// testUser.setName("testName");
		testUser.setUsername("testUsername");

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
	}

	@Test
	public void createUser_validInputs_success() {
		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		User createdUser = userService.createUser(testUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(testUser.getId(), createdUser.getId());
		// assertEquals(testUser.getName(), createdUser.getName());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
	}

	// @Test
	// public void createUser_duplicateName_throwsException() {
	// 	// given -> a first user has already been created
	// 	userService.createUser(testUser);

	// 	// when -> setup additional mocks for UserRepository
	// 	// Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
	// 	Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

	// 	// then -> attempt to create second user with same user -> check that an error
	// 	// is thrown
	// 	assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	// }

	@Test
	public void createUser_duplicateInputs_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		// Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}



    @Test
    public void updateUser_setsAllowPrivateMessages_success() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("alice");
        existingUser.setAllowPrivateMessages(true);

        Mockito.when(userRepository.findById(1L))
                .thenReturn(java.util.Optional.of(existingUser));

        User updates = new User();
        updates.setAllowPrivateMessages(false);

        User result = userService.updateUser(1L, updates);

        assertEquals(false, result.getAllowPrivateMessages());
        Mockito.verify(userRepository, Mockito.times(1)).save(existingUser);
    }

	@Test
	public void followUser_validInput_success() {
		// given
		User follower = new User();
		follower.setId(1L);
		follower.setUsername("follower");
		follower.setFollowing(new HashSet<>());

		User target = new User();
		target.setId(2L);
		target.setUsername("target");

		Mockito.when(userRepository.findById(1L))
				.thenReturn(Optional.of(follower));

		Mockito.when(userRepository.findById(2L))
				.thenReturn(Optional.of(target));

		Mockito.when(userRepository.save(Mockito.any(User.class)))
				.thenReturn(follower);

		// when
		User result = userService.followUser(1L, 2L);

		// then
		assertNotNull(result);

		Mockito.verify(userRepository, Mockito.times(1))
				.save(Mockito.any(User.class));

		assertTrue(
            follower.getFollowing().stream()
                    .anyMatch(user -> user.getId().equals(2L))
    	);
	}

	@Test
	public void followedUsers_getFollowingUsers_FollowingUsersReturned() {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");

		User followedUser = new User();
		followedUser.setId(2L);
		followedUser.setUsername("followedUsername");

		user.setFollowing(new HashSet<>(Set.of(followedUser)));

		Mockito.when(userRepository.findById(1L))
				.thenReturn(Optional.of(user));

		// when
		Set<User> result = userService.getFollowingUsers(1L);

		// then
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.equals(Set.of(followedUser)));
	}

	@Test
	public void getUsers_returnsAllUsers() {
		User user1 = new User();
		user1.setId(1L);
		User user2 = new User();
		user2.setId(2L);
		Mockito.when(userRepository.findAll()).thenReturn(List.of(user1, user2));

		List<User> result = userService.getUsers();

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	public void getUser_found_returnsUser() {
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		User result = userService.getUser(1L);

		assertNotNull(result);
		assertEquals(1L, result.getId());
	}

	@Test
	public void getUser_notFound_throwsNotFound() {
		Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> userService.getUser(99L));
	}

	@Test
	public void unfollowUser_validInput_success() {
		User follower = new User();
		follower.setId(1L);
		follower.setUsername("follower");

		User target = new User();
		target.setId(2L);
		target.setUsername("target");

		follower.setFollowing(new HashSet<>(Set.of(target)));

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
		Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(target));
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(follower);

		User result = userService.unfollowUser(1L, 2L);

		assertNotNull(result);
		assertFalse(follower.getFollowing().stream().anyMatch(u -> u.getId().equals(2L)));
		Mockito.verify(userRepository, Mockito.times(1)).save(follower);
	}

	@Test
	public void unfollowUser_selfUnfollow_throwsBadRequest() {
		ResponseStatusException ex = assertThrows(
				ResponseStatusException.class,
				() -> userService.unfollowUser(1L, 1L));
		assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	public void unfollowUser_notFollowing_throwsConflict() {
		User follower = new User();
		follower.setId(1L);
		follower.setFollowing(new HashSet<>());

		User target = new User();
		target.setId(2L);

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
		Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(target));

		ResponseStatusException ex = assertThrows(
				ResponseStatusException.class,
				() -> userService.unfollowUser(1L, 2L));
		assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
	}

	@Test
	public void getFollowers_returnsFollowers() {
		User user = new User();
		user.setId(2L);

		User follower = new User();
		follower.setId(1L);
		follower.setFollowing(new HashSet<>(Set.of(user)));

		User nonFollower = new User();
		nonFollower.setId(3L);
		nonFollower.setFollowing(new HashSet<>());

		Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		Mockito.when(userRepository.findAll()).thenReturn(List.of(follower, nonFollower));

		List<User> result = userService.getFollowers(2L);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(1L, result.get(0).getId());
	}

}
