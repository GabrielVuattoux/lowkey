package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.PublicEventGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.EventService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Public events, browsable without an account.
 * Clients send the geohash cells around them, never their coordinates
 * (see docs/architecture.md, section 4.4).
 */
@RestController
@RequestMapping("/public/events")
public class PublicEventController {
    private final EventService eventService;

    PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    // GET /public/events?cells=u0qjd,u0qje
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<PublicEventGetDTO> getPublicEvents(@RequestParam List<String> cells) {
        return eventService.getPublicEventsInCells(cells).stream()
                .map(DTOMapper.INSTANCE::convertEntityToPublicEventGetDTO)
                .toList();
    }
}
