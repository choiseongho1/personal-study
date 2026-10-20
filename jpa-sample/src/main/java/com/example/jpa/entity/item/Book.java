package com.example.jpa.entity.item;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("BOOK")
@Getter @Setter
public class Book extends Item {

    private String author;
    private String isbn;

    // getter/setter
}
