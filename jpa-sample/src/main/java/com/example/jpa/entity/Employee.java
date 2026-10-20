package com.example.jpa.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "city", column = @Column(name = "home_city")),
        @AttributeOverride(name = "street", column = @Column(name = "home_street")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "home_zip"))
    })
    private WorkAddress homeAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "city", column = @Column(name = "work_city")),
        @AttributeOverride(name = "street", column = @Column(name = "work_street")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "work_zip"))
    })
    private WorkAddress workAddress;

    @ElementCollection
    @CollectionTable(name = "employee_hobbies", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "hobby")
    private List<String> hobbies = new ArrayList<>();


    protected Employee() {}

    public Employee(String name, WorkAddress homeAddress, WorkAddress workAddress) {
        this.name = name;
        this.homeAddress = homeAddress;
        this.workAddress = workAddress;
    }

    // getter/setter
}
