package ch.uzh.ifi.hase.soprafs26.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ch.uzh.ifi.hase.soprafs26.entity.Message;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.MessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MessageGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MessagePostDTO;
import ch.uzh.ifi.hase.soprafs26.service.MessageService;

@WebMvcTest(MessageController.class)
public class MessageControllerTest {
    @Autowired
	private MockMvc mockMvc;

    @MockitoBean
	private MessageService messageService;

	@MockitoBean
	private MessageRepository messageRepository;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private UserRepository userRepository;


    @BeforeEach
    public void setupAuthentication() {
        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        given(userRepository.findByToken(anyString())).willReturn(authenticatedUser);
    }


    @Test
    public void getChatHistory_whenEventFound_returnsMessages() throws Exception {
        // given
        Message message = new Message();
        message.setContent("Hello");
        message.setSender(new User());
        given(messageService.getChatHistory(eq(1L), any(User.class)))
        .willReturn(List.of(message));
        
        MockHttpServletRequestBuilder getRequest = get("/events/1/messages")
                .header("Authorization", "Bearer valid-token");

        // when then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("Hello")));
    }

    @Test
    public void getChatHistory_whenUserNotParticipant_returns403() throws Exception {
        // given
        given(messageService.getChatHistory(eq(1L), any(User.class)))
            .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant"));

        MockHttpServletRequestBuilder request = get("/events/1/messages")
            .header("Authorization", "Bearer valid-token");

        // when + then
        mockMvc.perform(request)
            .andExpect(status().isForbidden());
    }

    // Testing the websocket
    @Test
    void sendMessage_returnsDTO() {
        // given
        MessagePostDTO input = new MessagePostDTO();
        input.setContent("Hello");

        Message message = new Message();
        message.setContent("Hello");

        given(messageService.saveMessage(input)).willReturn(message);

        MessageController controller = new MessageController(messageService);

        // when
        MessageGetDTO result = controller.sendMessage(input);

        // then
        assertEquals("Hello", result.getContent());
    }

    
}
