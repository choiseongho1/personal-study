package com.randombox.api.v1.user.dto;

import com.randombox.domain.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class UserResponse {

    @Getter
    public static class UserInfo {
        private final Long id;
        private final String email;
        private final String nickname;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        @Builder
        public UserInfo(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.nickname = user.getNickname();
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
        }
    }
}
