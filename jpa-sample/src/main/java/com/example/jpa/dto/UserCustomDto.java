package com.example.jpa.dto;

import com.querydsl.core.annotations.QueryProjection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Builder
@Getter @Setter
@NoArgsConstructor
public class UserCustomDto {

    private Long id;
    private String name;
    private String email;
    private List<OrderCustomDto> orders;

    @QueryProjection
    public UserCustomDto(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    @QueryProjection
    public UserCustomDto(Long id, String name, String email, List<OrderCustomDto> orders) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.orders = orders;
    }
}
