package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Rating;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RatingPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.RatingService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
public class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private UserRepository userRepository;

    private User rater;
    private User organizer;
    private Event event;
    private Rating rating;

    @BeforeEach
    public void setup() {
        rater = new User();
        rater.setId(2L);
        rater.setUsername("participant");
        rater.setToken("test-token");

        organizer = new User();
        organizer.setId(1L);
        organizer.setUsername("organizer");

        event = new Event();
        event.setId(10L);
        event.setCreator(organizer);
        event.setEndTime(LocalDateTime.now().minusHours(1));

        rating = new Rating();
        rating.setId(100L);
        rating.setRater(rater);
        rating.setOrganizer(organizer);
        rating.setEvent(event);
        rating.setScore(4);
        rating.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void rateOrganizer_validInput_returnsCreated() throws Exception {
        given(userRepository.findByToken(anyString())).willReturn(rater);
        given(ratingService.createRating(Mockito.eq(10L), Mockito.any(User.class), Mockito.eq(4)))
                .willReturn(rating);

        RatingPostDTO dto = new RatingPostDTO();
        dto.setScore(4);

        MockHttpServletRequestBuilder postRequest = post("/events/10/ratings")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(rating.getId().intValue())))
                .andExpect(jsonPath("$.score", is(rating.getScore())))
                .andExpect(jsonPath("$.raterId", is(rater.getId().intValue())))
                .andExpect(jsonPath("$.organizerId", is(organizer.getId().intValue())));
    }

    @Test
    public void rateOrganizer_missingToken_returnsUnauthorized() throws Exception {
        RatingPostDTO dto = new RatingPostDTO();
        dto.setScore(4);

        MockHttpServletRequestBuilder postRequest = post("/events/10/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void rateOrganizer_invalidScore_returnsBadRequest() throws Exception {
        given(userRepository.findByToken(anyString())).willReturn(rater);

        RatingPostDTO dto = new RatingPostDTO();
        dto.setScore(6);

        MockHttpServletRequestBuilder postRequest = post("/events/10/ratings")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void rateOrganizer_duplicateRating_returnsConflict() throws Exception {
        given(userRepository.findByToken(anyString())).willReturn(rater);
        given(ratingService.createRating(Mockito.eq(10L), Mockito.any(User.class), Mockito.eq(4)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "You have already rated this event"));

        RatingPostDTO dto = new RatingPostDTO();
        dto.setScore(4);

        MockHttpServletRequestBuilder postRequest = post("/events/10/ratings")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void getMyRating_existingRating_returnsRating() throws Exception {
        given(userRepository.findByToken(anyString())).willReturn(rater);
        given(ratingService.getMyRating(Mockito.any(User.class), Mockito.eq(10L)))
                .willReturn(rating);

        MockHttpServletRequestBuilder getRequest = get("/events/10/ratings/me")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(rating.getScore())))
                .andExpect(jsonPath("$.raterId", is(rater.getId().intValue())));
    }

    @Test
    public void getMyRating_noRating_returnsOkWithNullBody() throws Exception {
        given(userRepository.findByToken(anyString())).willReturn(rater);
        given(ratingService.getMyRating(Mockito.any(User.class), Mockito.eq(10L)))
                .willReturn(null);

        MockHttpServletRequestBuilder getRequest = get("/events/10/ratings/me")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void getMyRating_missingToken_returnsUnauthorized() throws Exception {
        MockHttpServletRequestBuilder getRequest = get("/events/10/ratings/me")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
