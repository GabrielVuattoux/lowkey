package ch.uzh.ifi.hase.soprafs26.controller;


import ch.uzh.ifi.hase.soprafs26.authentication.AuthenticatedUser;
import ch.uzh.ifi.hase.soprafs26.constant.EventCategory;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventJoinByCodePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.EventJoinByIdPostDTO;
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
        return DTOMapper.INSTANCE.convertEntityToEventGetDTO(newEvent);
    }

    // Join event by eventId (meant for joins through event search)
    @PostMapping("/{eventId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public EventGetDTO joinEventById(@AuthenticatedUser User user, @PathVariable Long eventId, @RequestBody EventJoinByIdPostDTO eventJoinByIdPostDTO) {
        Event event = participantService.joinEventById(eventId, eventJoinByIdPostDTO.getUserId());
        return DTOMapper.INSTANCE.convertEntityToEventGetDTO(event);
    }

    // Join event by inviteCode (meant for joins through invitations)
    @PostMapping("/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public EventGetDTO joinEventBynviteCode(@AuthenticatedUser User user, @RequestBody EventJoinByCodePostDTO eventJoinByCodePostDTO) {
        Event event = participantService.joinEventByInviteCode(eventJoinByCodePostDTO.getInviteCode(), eventJoinByCodePostDTO.getUserId());
        return DTOMapper.INSTANCE.convertEntityToEventGetDTO(event);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventGetDTO getEventById(@AuthenticatedUser User user, @PathVariable Long id) {
        Event event = eventService.getEventById(id);
        return DTOMapper.INSTANCE.convertEntityToEventGetDTO(event);
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
            EventGetDTO dto = DTOMapper.INSTANCE.convertEntityToEventGetDTO(event);
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
        participantService.removeParticipantFromEvent(userId, eventId);
    }


    @GetMapping("/{eventId}/participants")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getEventParticipants(
            @AuthenticatedUser User user,
            @PathVariable Long eventId
    ) {
        List<User> participants = eventService.getEventParticipants(eventId);

        return participants.stream()
                .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                .toList();
    }
    

}
