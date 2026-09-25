package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Post;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PostGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PostPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/events/{eventId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostGetDTO createPost(@PathVariable Long eventId, @RequestBody PostPostDTO postPostDTO) {
        postPostDTO.setEventId(eventId);
        Post post = postService.createPost(postPostDTO);
        return DTOMapper.INSTANCE.convertEntityToPostGetDTO(post);
    }

    @GetMapping("/events/{eventId}/posts")
    public List<PostGetDTO> getPosts(@PathVariable Long eventId, @RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        List<PostGetDTO> result = new ArrayList<>();
        for (Post post : postService.getPostsByEvent(eventId, token)) {
            result.add(DTOMapper.INSTANCE.convertEntityToPostGetDTO(post));
        }
        return result;
    }

    @DeleteMapping("/events/{eventId}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long eventId, @PathVariable Long postId,
                           @RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        postService.deletePost(eventId, postId, token);
    }
}
