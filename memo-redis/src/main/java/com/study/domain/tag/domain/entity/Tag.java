package com.study.domain.tag.domain.entity;

import com.study.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Comment;


@Entity
@Table(name = "TAG")
@Builder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    @Comment("태그 ID (Auto Increment)")
    private Long id;

    @Column(name = "NAME", unique = true)
    @Comment("태그명")
    private String name;


}
