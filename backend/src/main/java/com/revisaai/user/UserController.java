package com.revisaai.user;

import com.revisaai.user.dto.SessionSummary;
import com.revisaai.user.dto.UserStatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getStats(Authentication authentication) {
        return ResponseEntity.ok(userService.getStats(authentication.getName()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SessionSummary>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(userService.getHistory(authentication.getName()));
    }
}
