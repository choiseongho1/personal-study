package com.example.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Task {

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.EAGER) // 👈 여기도 실험 가능
    @JoinColumn(name = "project_id")
    private Project project;

    public Task() {}
    public Task(String title, Project project) {
        this.title = title;
        this.project = project;
    }

    // getter/setter 생략
}
