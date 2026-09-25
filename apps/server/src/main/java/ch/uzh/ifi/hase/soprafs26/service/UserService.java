package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User getUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	public User createUser(User newUser) {
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.ONLINE);
		checkIfUserExists(newUser);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public User loginUser(User loginAttempt) {
		checkUserCredentials(loginAttempt);

		User user = userRepository.findByUsername(loginAttempt.getUsername());
		
		user.setStatus(UserStatus.ONLINE);

		userRepository.save(user);
		userRepository.flush();

		log.debug("Information of logged-in User: {}", user);
		return user;
	}

	public User updateUser(Long id, User userUpdates) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (userUpdates.getUsername() != null && !userUpdates.getUsername().equals(user.getUsername())) {
			User existing = userRepository.findByUsername(userUpdates.getUsername());
			if (existing != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
			}
			user.setUsername(userUpdates.getUsername());
		}
		if (userUpdates.getEmail() != null && !userUpdates.getEmail().equals(user.getEmail())) {
			User existing = userRepository.findByEmail(userUpdates.getEmail());
			if (existing != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already taken");
			}
			user.setEmail(userUpdates.getEmail());
		}
        if (userUpdates.getBio() != null) {
            user.setBio(userUpdates.getBio());
        }
        if (userUpdates.getAllowPrivateMessages() != null) {
            user.setAllowPrivateMessages(userUpdates.getAllowPrivateMessages());
        }
		if (userUpdates.getStatus() != null) {
			user.setStatus(userUpdates.getStatus());
		}
		if (userUpdates.getPassword() != null) {
			user.setPassword(userUpdates.getPassword());
		}

		userRepository.save(user);
		userRepository.flush();
		return user;
	}

	public User followUser(Long followerId, Long followeeId) {
		if (followerId.equals(followeeId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot follow yourself");
		}

		User follower = userRepository.findById(followerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follower not found"));

		User followee = userRepository.findById(followeeId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Followee not found"));

		// Check via ID (sicherer als contains)
		boolean alreadyFollowing = follower.getFollowing().stream()
			.anyMatch(user -> user.getId().equals(followeeId));

		if (alreadyFollowing) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following this user");
		}

		follower.getFollowing().add(followee);

		return userRepository.save(follower);
	}

	public User unfollowUser(Long followerId, Long followeeId) {
		if (followerId.equals(followeeId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot unfollow yourself");
		}

		User follower = userRepository.findById(followerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follower not found"));

		userRepository.findById(followeeId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Followee not found"));

		boolean removed = follower.getFollowing().removeIf(
			user -> user.getId().equals(followeeId)
		);

		if (!removed) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You are not following this user");
		}

		return userRepository.save(follower);
	}

	public Set<User> getFollowingUsers(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		return user.getFollowing();
	}

	public List<User> getFollowers(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"User not found"
				));

		return userRepository.findAll().stream()
				.filter(possibleFollower ->
						possibleFollower.getFollowing().stream()
								.anyMatch(followedUser ->
										followedUser.getId().equals(user.getId())
								)
				)
				.toList();
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		User userByEmail = userToBeCreated.getEmail() != null
				? userRepository.findByEmail(userToBeCreated.getEmail())
				: null;

		if (userByUsername != null && userByEmail != null && userByUsername.getId().equals(userByEmail.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this username and email already exists. Please log in instead.");
		}
		String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
		}
		if (userByEmail != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "email", "is"));
		}
	}

	private void checkUserCredentials(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

		String baseErrorMessage = "The provided username or password is wrong!";

		if (userByUsername == null || !userByUsername.getPassword().equals(userToBeCreated.getPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage));
		} 
	}


}
