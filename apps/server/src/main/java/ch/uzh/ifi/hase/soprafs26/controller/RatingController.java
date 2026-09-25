package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.authentication.AuthenticatedUser;
import ch.uzh.ifi.hase.soprafs26.entity.Rating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RatingGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RatingPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/events/{eventId}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingGetDTO rateOrganizer(@AuthenticatedUser User user,
                                      @PathVariable Long eventId,
                                      @Valid @RequestBody RatingPostDTO dto) {
        Rating rating = ratingService.createRating(eventId, user, dto.getScore());
        return DTOMapper.INSTANCE.convertEntityToRatingGetDTO(rating);
    }

    @GetMapping("/events/{eventId}/ratings/me")
    public RatingGetDTO getMyRating(@AuthenticatedUser User user, @PathVariable Long eventId) {
        Rating r = ratingService.getMyRating(user, eventId);
        return r == null ? null : DTOMapper.INSTANCE.convertEntityToRatingGetDTO(r);
    }
}