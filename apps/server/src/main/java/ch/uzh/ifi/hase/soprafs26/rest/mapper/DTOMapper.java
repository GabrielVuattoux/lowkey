package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	// @Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "password", target = "password")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "token", ignore = true)
	@Mapping(target = "status", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "allowPrivateMessages", ignore = true)
    @Mapping(target = "following", ignore = true)
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "allowPrivateMessages", target = "allowPrivateMessages")
    @Mapping(source = "status", target = "status")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "token", ignore = true)
    @Mapping(target = "following", ignore = true)
	User convertUserPutDTOtoEntity(UserPutDTO userPutDTO);

	@Mapping(source = "id", target = "id")
	// @Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "status", target = "status")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "allowPrivateMessages", target = "allowPrivateMessages")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(
        target = "followingIds",
        expression = "java(user.getFollowing() != null ? user.getFollowing().stream().map(ch.uzh.ifi.hase.soprafs26.entity.User::getId).collect(java.util.stream.Collectors.toSet()) : new java.util.HashSet<>())"
    )
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "token", target = "token")
	UserGetTokenDTO convertEntityToUserGetTokenDTO(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    UserGetPreviewDTO convertEntityToUserGetPreviewDTO(User user);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "title", target = "title")
	@Mapping(source = "description", target = "description")
	@Mapping(source = "startTime", target = "startTime")
	@Mapping(source = "endTime", target = "endTime")
	@Mapping(source = "latitude", target = "latitude")
	@Mapping(source = "longitude", target = "longitude")
	@Mapping(source = "isPrivate", target = "isPrivate")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "creator.id", target = "creatorId")
    @Mapping(source = "creator.username", target = "creatorUsername")
    @Mapping(target = "participantCount", expression = "java(event.getParticipants() != null ? event.getParticipants().size() : 0)")
    @Mapping(target = "participantIds", expression = "java(event.getParticipants() != null ? event.getParticipants().stream().map(ch.uzh.ifi.hase.soprafs26.entity.User::getId).collect(java.util.stream.Collectors.toList()) : new java.util.ArrayList<>())")
    @Mapping(source = "pictureUrls", target = "pictureUrls")
    @Mapping(source = "inviteCode", target = "inviteCode")
    @Mapping(source = "cancelledAt", target = "cancelledAt")
    @Mapping(target = "isParticipant", ignore = true)
	EventGetDTO convertEntityToEventGetDTO(Event event);


    @Mapping(source = "title", target = "title")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "startTime", target = "startTime")
    @Mapping(source = "endTime", target = "endTime")
    @Mapping(source = "latitude", target = "latitude")
    @Mapping(source = "longitude", target = "longitude")
    @Mapping(source = "isPrivate", target = "isPrivate")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "pictureUrls", target = "pictureUrls")
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inviteCode", ignore = true)
    Event convertEventPostDTOtoEntity(EventPostDTO eventPostDTO);

	@Mapping(source = "title", target = "title")
	@Mapping(source = "description", target = "description")
	@Mapping(source = "startTime", target = "startTime")
	@Mapping(source = "endTime", target = "endTime")
	@Mapping(source = "latitude", target = "latitude")
	@Mapping(source = "longitude", target = "longitude")
	@Mapping(source = "isPrivate", target = "isPrivate")
	@Mapping(source = "category", target = "category")
	@Mapping(source = "pictureUrls", target = "pictureUrls")
	@Mapping(target = "creator", ignore = true)
	@Mapping(target = "participants", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inviteCode", ignore = true)
	Event convertEventPutDTOtoEntity(EventPutDTO eventPutDTO);

    @Mapping(source = "sender.username", target = "senderUsername")
    @Mapping(source = "event.id", target = "eventId")
    MessageGetDTO convertEntityToMessageGetDTO(Message message);

    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Message convertMessagePostDTOtoEntity(MessagePostDTO messagePostDTO);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "event.id", target = "eventId")
    PostGetDTO convertEntityToPostGetDTO(Post post);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "rater.id", target = "raterId")
    @Mapping(source = "organizer.id", target = "organizerId")
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "score", target = "score")
    @Mapping(source = "createdAt", target = "createdAt")
    RatingGetDTO convertEntityToRatingGetDTO(Rating rating);
}
