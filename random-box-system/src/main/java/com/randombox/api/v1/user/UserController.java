package com.randombox.api.v1.user;

import com.randombox.api.v1.user.dto.UserRequest;
import com.randombox.api.v1.user.dto.UserResponse;
import com.randombox.domain.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse.UserInfo> signup(@Valid @RequestBody UserRequest.SignUp request) {
        return ResponseEntity.ok(userService.signup(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse.UserInfo> getUserInfo(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<UserResponse.UserInfo> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest.UpdatePassword request) {
        return ResponseEntity.ok(userService.updatePassword(userId, request));
    }

    @PatchMapping("/{userId}/nickname")
    public ResponseEntity<UserResponse.UserInfo> updateNickname(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest.UpdateNickname request) {
        return ResponseEntity.ok(userService.updateNickname(userId, request));
    }
}
