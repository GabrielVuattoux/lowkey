package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Event;
import ch.uzh.ifi.hase.soprafs26.entity.Post;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.EventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PostRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PostPostDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PostService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PostRepository postRepository;

    public PostService(
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("eventRepository") EventRepository eventRepository,
            @Qualifier("postRepository") PostRepository postRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
    }

    private User getUserByToken(String token) {
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return user;
    }

    private Event getEventIfParticipant(Long eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Event with id %d does not exist", eventId)));
        if (!event.getParticipants().contains(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a participant of this event");
        }
        return event;
    }

    public Post createPost(PostPostDTO dto) {
        User author = getUserByToken(dto.getToken());
        Event event = getEventIfParticipant(dto.getEventId(), author);

        Post post = new Post();
        post.setPostType(dto.getPostType());
        post.setContent(dto.getContent());
        post.setImageUrl(dto.getImageUrl());
        post.setEmoji(dto.getEmoji());
        post.setAuthor(author);
        post.setEvent(event);
        post.setTimestamp(LocalDateTime.now());

        return postRepository.save(post);
    }

    public List<Post> getPostsByEvent(Long eventId, String token) {
        getUserByToken(token);
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Event with id %d does not exist", eventId));
        }
        return postRepository.findByEventIdOrderByTimestampAsc(eventId);
    }

    public void deletePost(Long eventId, Long postId, String token) {
        User user = getUserByToken(token);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Post with id %d does not exist", postId)));
        if (!post.getEvent().getId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Post with id %d does not belong to event %d", postId, eventId));
        }
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own posts");
        }
        getEventIfParticipant(eventId, user);
        postRepository.delete(post);
    }
}
