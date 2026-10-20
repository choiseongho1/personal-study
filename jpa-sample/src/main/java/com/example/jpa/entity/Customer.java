package com.example.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * explain the Class File
 *
 * @author : choiseongho
 * @fileName : Member.java
 * @since : 2025-04-04
 */

@Entity
@Getter @Setter
@AllArgsConstructor
@Builder
public class Customer {

    @Id @GeneratedValue
    private Long id;

    private String name;

    private String phone;

    protected Customer() {}
    public Customer(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
