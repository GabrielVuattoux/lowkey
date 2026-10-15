package ch.uzh.ifi.hase.soprafs26.controller;


import ch.uzh.ifi.hase.soprafs26.authentication.AuthenticatedUser;
import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventJoinByCodePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.EventService;
import ch.uzh.ifi.hase.soprafs26.service.ParticipantService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/events")
public class EventController {
    private final EventService eventService;

    private final ParticipantService participantService;

    EventController(EventService eventService, ParticipantService participantService) {
        this.eventService = eventService;
        this.participantService = participantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public EventGetDTO createEvent(@AuthenticatedUser User user, @Valid @RequestBody EventPostDTO eventPostDTO){
        Event eventData = DTOMapper.INSTANCE.convertEventPostDTOtoEntity(eventPostDTO);
        Event newEvent = eventService.createEvent(eventData, user);
        return DTOMapper.INSTANCE.convertEntityToEventGetDTOFor(newEvent, user);
    }

    // Join event by eventId (meant for joins through event search). Always joins as the
    // authenticated user, never as a user id taken from the request.
    @PostMapping("/{eventId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public EventGetDTO joinEventById(@AuthenticatedUser User user, @PathVariable Long eventId) {
        Event event = participantService.joinEventById(eventId, user.getId());
        return DTOMapper.INSTANCE.convertEntityToEventGetDTOFor(event, user);
    }

    // Join event by inviteCode (meant for joins through invitations)
    @PostMapping("/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public EventGetDTO joinEventBynviteCode(@AuthenticatedUser User user, @RequestBody EventJoinByCodePostDTO eventJoinByCodePostDTO) {
        Event event = participantService.joinEventByInviteCode(eventJoinByCodePostDTO.getInviteCode(), user.getId());
        return DTOMapper.INSTANCE.convertEntityToEventGetDTOFor(event, user);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventGetDTO getEventById(@AuthenticatedUser User user, @PathVariable Long id) {
        Event event = getVisibleEvent(id, user);
        return DTOMapper.INSTANCE.convertEntityToEventGetDTOFor(event, user);
    }

    @GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<EventGetDTO> getRadiusEvents(
            @AuthenticatedUser User user,
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam double radius,
        @RequestParam(required = false) Set<EventCategory> categories,
        @RequestParam(required = false, defaultValue = "false") boolean includePast) {

        List<Event> events = eventService.getEventsInRadius(latitude, longitude, radius, user, categories, includePast);
        List<EventGetDTO> eventGetDTOs = new ArrayList<>();

        for (Event event : events) {
            EventGetDTO dto = DTOMapper.INSTANCE.convertEntityToEventGetDTOFor(event, user);
            boolean isParticipant = event.getParticipants() != null && user != null &&
                event.getParticipants().stream()
                    .anyMatch(u -> u.getId().equals(user.getId()));
            dto.setIsParticipant(isParticipant);
            eventGetDTOs.add(dto);
		}

		return eventGetDTOs;

	}

    @PutMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEvent(@AuthenticatedUser User user, @PathVariable Long eventId, @RequestBody EventPutDTO eventPutDTO) {
        Event eventData = DTOMapper.INSTANCE.convertEventPutDTOtoEntity(eventPutDTO);
        eventService.updateEvent(eventId, eventData, user);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@AuthenticatedUser User user, @PathVariable Long eventId) {
        eventService.deleteEvent(eventId, user);
    }


    @DeleteMapping("/{eventId}/participants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveEvent(@AuthenticatedUser User user, @PathVariable Long eventId, @PathVariable long userId) {
        if (userId != user.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove yourself from an event");
        }
        participantService.removeParticipantFromEvent(userId, eventId);
    }


    @GetMapping("/{eventId}/participants")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getEventParticipants(
            @AuthenticatedUser User user,
            @PathVariable Long eventId
    ) {
        getVisibleEvent(eventId, user);
        List<User> participants = eventService.getEventParticipants(eventId);

        return participants.stream()
                .map(participant -> DTOMapper.INSTANCE.convertEntityToUserGetDTOFor(participant, user))
                .toList();
    }

    // A private event does not exist for non-participants: 404 rather than 403,
    // so that its existence is not confirmed either
    private Event getVisibleEvent(Long eventId, User user) {
        Event event = eventService.getEventById(eventId);
        if (Boolean.TRUE.equals(event.getIsPrivate()) && !event.hasParticipant(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        return event;
    }
    

}
