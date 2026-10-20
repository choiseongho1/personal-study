package com.example.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor @NoArgsConstructor
public class Comment extends BaseTimeEntity{
    @Id
    @GeneratedValue
    private Long id;

    private String writer;
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

}
