package com.randombox.domain.user;

import com.randombox.api.v1.user.dto.UserRequest;
import com.randombox.api.v1.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse.UserInfo signup(UserRequest.SignUp request) {
        validateSignup(request);

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .build();

        user.encodePassword(passwordEncoder);
        User savedUser = userRepository.save(user);

        return new UserResponse.UserInfo(savedUser);
    }

    private void validateSignup(UserRequest.SignUp request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }
    }

    public UserResponse.UserInfo getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return new UserResponse.UserInfo(user);
    }

    @Transactional
    public UserResponse.UserInfo updatePassword(Long userId, UserRequest.UpdatePassword request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        user.updatePassword(request.getNewPassword(), passwordEncoder);
        return new UserResponse.UserInfo(user);
    }

    @Transactional
    public UserResponse.UserInfo updateNickname(Long userId, UserRequest.UpdateNickname request) {
        if (userRepository.existsByNickname(request.getNewNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        user.updateNickname(request.getNewNickname());
        return new UserResponse.UserInfo(user);
    }
}
