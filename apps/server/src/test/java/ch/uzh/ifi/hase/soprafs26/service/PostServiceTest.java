package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.PostType;
import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Post;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PostRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PostPostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PostServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private Event testEvent;
    private PostPostDTO testDto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testEvent = new Event();
        testEvent.setId(1L);
        List<User> participants = new ArrayList<>();
        participants.add(testUser);
        testEvent.setParticipants(participants);

        testDto = new PostPostDTO();
        testDto.setToken("valid-token");
        testDto.setEventId(1L);
        testDto.setPostType(PostType.COMMENT);
        testDto.setContent("Hello World");
    }

    @Test
    public void createPost_validInput_success() {
        // given
        Post savedPost = new Post();
        savedPost.setId(1L);
        savedPost.setContent("Hello World");
        savedPost.setPostType(PostType.COMMENT);
        savedPost.setAuthor(testUser);
        savedPost.setEvent(testEvent);
        savedPost.setTimestamp(LocalDateTime.now());

        Long eventId = 1L;
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // when
        Post result = postService.createPost(testDto);

        // then
        assertNotNull(result);
        assertEquals("Hello World", result.getContent());
        assertEquals(PostType.COMMENT, result.getPostType());
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    public void createPost_invalidToken_throwsUnauthorized() {
        // given
        when(userRepository.findByToken("invalid-token")).thenReturn(null);
        testDto.setToken("invalid-token");

        // when + then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> postService.createPost(testDto));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void createPost_eventNotFound_throwsNotFound() {
        // given
        Long eventId = 1L;
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // when + then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> postService.createPost(testDto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void createPost_userNotParticipant_throwsForbidden() {
        // given
        Event eventWithoutUser = new Event();
        eventWithoutUser.setId(1L);
        eventWithoutUser.setParticipants(new ArrayList<>());

        Long eventId = 1L;
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithoutUser));

        // when + then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> postService.createPost(testDto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    public void createPost_setsTimestamp_notNull() {
        // given
        Post savedPost = new Post();
        savedPost.setId(1L);
        savedPost.setTimestamp(LocalDateTime.now());

        Long eventId = 1L;
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            assertNotNull(p.getTimestamp());
            return savedPost;
        });

        // when + then
        postService.createPost(testDto);
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    public void getPostsByEvent_validInput_returnsPosts() {
        // given
        Post post = new Post();
        post.setId(1L);
        post.setContent("Hello");

        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(postRepository.findByEventIdOrderByTimestampAsc(1L)).thenReturn(List.of(post));

        // when
        List<Post> result = postService.getPostsByEvent(1L, "valid-token");

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0).getContent());
    }

    @Test
    public void getPostsByEvent_emptyEvent_returnsEmptyList() {
        // given
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(postRepository.findByEventIdOrderByTimestampAsc(1L)).thenReturn(new ArrayList<>());

        // when
        List<Post> result = postService.getPostsByEvent(1L, "valid-token");

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getPostsByEvent_invalidToken_throwsUnauthorized() {
        // given
        when(userRepository.findByToken("bad-token")).thenReturn(null);

        // when + then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> postService.getPostsByEvent(1L, "bad-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getPostsByEvent_eventNotFound_throwsNotFound() {
        // given
        when(userRepository.findByToken("valid-token")).thenReturn(testUser);
        when(eventRepository.existsById(99L)).thenReturn(false);

        // when + then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> postService.getPostsByEvent(99L, "valid-token"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
