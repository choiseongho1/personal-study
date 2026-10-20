package com.example.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Project {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(mappedBy = "project", fetch = FetchType.EAGER) // 👈 여기서 EAGER로 바꿔보며 비교
    private List<Task> tasks = new ArrayList<>();

    public Project() {}
    public Project(String name) {
        this.name = name;
    }

    // getter/setter 생략
}
