package com.example.jpa.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
public class UserSearchDto{
    private Long id;
    private String name;
    private String email;

    @QueryProjection
    public UserSearchDto(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}
