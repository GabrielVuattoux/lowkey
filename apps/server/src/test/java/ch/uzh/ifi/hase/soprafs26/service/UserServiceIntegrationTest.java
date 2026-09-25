package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
public class UserServiceIntegrationTest {

	@Qualifier("userRepository")
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@BeforeEach
	public void setup() {
		userRepository.deleteAll();
	}

	@Test
	public void createUser_validInputs_success() {
		// given
		assertNull(userRepository.findByUsername("testUsername"));

		User testUser = new User();
		// testUser.setName("testName");
		testUser.setUsername("testUsername");
		testUser.setEmail("test@example.com");
		testUser.setPassword("testPassword");

		// when
		User createdUser = userService.createUser(testUser);

		// then
		assertEquals(testUser.getId(), createdUser.getId());
		// assertEquals(testUser.getName(), createdUser.getName());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertEquals(testUser.getPassword(), createdUser.getPassword());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
	}

	@Test
	public void createUser_duplicateUsername_throwsException() {
		assertNull(userRepository.findByUsername("testUsername"));

		User testUser = new User();
		// testUser.setName("testName");
		testUser.setUsername("testUsername");
		testUser.setEmail("test1@example.com");
		testUser.setPassword("testPassword");
		userService.createUser(testUser);

		// attempt to create second user with same username
		User testUser2 = new User();
		testUser2.setPassword("testPassword2");
		testUser2.setEmail("test2@example.com");

		// change the username to the same one
		// testUser2.setName("testName2");
		testUser2.setUsername("testUsername");

		// check that an error is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
	}

	@Test
	public void loginUser_validCredentials_success() {
		// given
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("login.valid@example.com");
		testUser.setPassword("testPassword");
		userService.createUser(testUser);

		User loginAttempt = new User();
		loginAttempt.setUsername("testUsername");
		loginAttempt.setPassword("testPassword");

		// when
		User loggedInUser = userService.loginUser(loginAttempt);

		// then
		assertEquals(testUser.getId(), loggedInUser.getId());
		assertEquals(testUser.getUsername(), loggedInUser.getUsername());
		assertEquals(testUser.getPassword(), loggedInUser.getPassword());
		assertNotNull(loggedInUser.getToken());
		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
	}

	@Test
	public void loginUser_invalidCredentials_throwsException() {
		// given
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("login.invalid@example.com");
		testUser.setPassword("testPassword");
		userService.createUser(testUser);

		User loginAttempt = new User();
		loginAttempt.setUsername("testUsername");
		loginAttempt.setPassword("wrongPassword");

		// check that an error is thrown
		assertThrows(ResponseStatusException.class, () -> userService.loginUser(loginAttempt));
	}
}
