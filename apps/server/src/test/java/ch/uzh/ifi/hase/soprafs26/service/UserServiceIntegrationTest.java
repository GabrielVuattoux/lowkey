package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
	}

	@Test
	public void createUser_storesPasswordHashedNotInClear() {
		User testUser = new User();
		testUser.setUsername("hashedUser");
		testUser.setEmail("hashed@example.com");
		testUser.setPassword("testPassword");

		userService.createUser(testUser);

		String stored = userRepository.findByUsername("hashedUser").getPassword();
		assertNotEquals("testPassword", stored);
		assertTrue(new BCryptPasswordEncoder().matches("testPassword", stored));
	}

	@Test
	public void createUser_withoutPassword_throwsException() {
		User testUser = new User();
		testUser.setUsername("noPassword");
		testUser.setEmail("no.password@example.com");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void updateUser_newPassword_isHashedAndUsableForLogin() {
		User testUser = new User();
		testUser.setUsername("changingUser");
		testUser.setEmail("changing@example.com");
		testUser.setPassword("oldPassword");
		User created = userService.createUser(testUser);

		User updates = new User();
		updates.setPassword("newPassword");
		userService.updateUser(created.getId(), updates);

		assertNotEquals("newPassword", userRepository.findByUsername("changingUser").getPassword());

		User oldLogin = new User();
		oldLogin.setUsername("changingUser");
		oldLogin.setPassword("oldPassword");
		assertThrows(ResponseStatusException.class, () -> userService.loginUser(oldLogin));

		User newLogin = new User();
		newLogin.setUsername("changingUser");
		newLogin.setPassword("newPassword");
		assertEquals(created.getId(), userService.loginUser(newLogin).getId());
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
