package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.authentication.AuthenticatedUser;
import ch.uzh.ifi.hase.soprafs26.service.RatingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetPreviewDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetTokenDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.EventService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

	private final UserService userService;
	private final EventService eventService;
    private final RatingService ratingService;

    UserController(UserService userService, EventService eventService, RatingService ratingService) {
        this.userService = userService;
        this.eventService = eventService;
        this.ratingService = ratingService;
    }
	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getAllUsers() {
		// fetch all users in the internal representation
		List<User> users = userService.getUsers();
		List<UserGetDTO> userGetDTOs = new ArrayList<>();

		// convert each user to the API representation
		for (User user : users) {
			userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
		}
		return userGetDTOs;
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetTokenDTO createUser(@RequestBody UserPostDTO userPostDTO) {
		// convert API user to internal representation
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// create user
		User createdUser = userService.createUser(userInput);
		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntityToUserGetTokenDTO(createdUser);
	}

	@PostMapping("/users/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetTokenDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
		// convert API user to internal representation
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// create user
		User createdUser = userService.loginUser(userInput);
		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntityToUserGetTokenDTO(createdUser);
	}

	@GetMapping("/auth/validate")
	@ResponseStatus(HttpStatus.OK)
	public void validateToken(@AuthenticatedUser User user) {
        // the @Authenticated User user handles tokens/user already no need to verify anything here no more
	}


    @GetMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getUserById(@PathVariable("id") Long id) {
        User user = userService.getUser(id);
        UserGetDTO dto = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
        dto.setAverageRating(ratingService.getAverageRating(user));
        dto.setRatingCount(ratingService.getRatingCount(user));
        return dto;
    }

	@PutMapping("/users/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO updateUser(@PathVariable("id") Long id, @RequestBody UserPutDTO userPutDTO) {
		User userUpdates = DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO);
		User updatedUser = userService.updateUser(id, userUpdates);
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
	}

	@GetMapping("/users/{id}/events")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<EventGetDTO> getUserEvents(@PathVariable("id") Long id) {
		List<Event> events = eventService.getEventsByUserId(id);
		List<EventGetDTO> dtos = new ArrayList<>();
		for (Event e : events) {
			dtos.add(DTOMapper.INSTANCE.convertEntityToEventGetDTO(e));
		}
		return dtos;
	}

	// follow users

	@GetMapping("/users/following")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Set<UserGetPreviewDTO> getFollowingUsers(@AuthenticatedUser User user) {
		Set<User> followingUsers = userService.getFollowingUsers(user.getId());
		Set<UserGetPreviewDTO> userGetPreviewDTOs = new java.util.HashSet<>();
		for (User u : followingUsers) {
			userGetPreviewDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetPreviewDTO(u));
		}
		return userGetPreviewDTOs;
	}

	@PostMapping("/users/{targetUserId}/follow")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO followUser(
			@AuthenticatedUser User follower,
			@PathVariable Long targetUserId) {

		User updatedUser = userService.followUser(follower.getId(),targetUserId);

		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
	}

	//unfollow
	@DeleteMapping("/users/{targetUserId}/follow")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO unfollowUser(
			@AuthenticatedUser User follower,
			@PathVariable Long targetUserId) {

		User updatedUser = userService.unfollowUser(follower.getId(), targetUserId);

		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
	}

	@GetMapping("/users/{userId}/following")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getFollowing(@PathVariable Long userId) {
		Set<User> following = userService.getFollowingUsers(userId);

		return following.stream()
			.map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
			.toList();
	}

	@GetMapping("/users/{userId}/followers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getFollowers(@PathVariable Long userId) {
		List<User> followers = userService.getFollowers(userId);

		return followers.stream()
				.map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
				.toList();
	}



}
