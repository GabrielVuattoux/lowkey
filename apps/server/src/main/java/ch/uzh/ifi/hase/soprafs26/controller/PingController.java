package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;


@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        System.out.println("PING ENDPOINT WAS CALLED");
        return ResponseEntity.ok("pong");
    }
}