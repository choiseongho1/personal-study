package com.example.jpa.entity.item;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("ALBUM")
@Getter
@Setter

public class Album extends Item {

    private String artist;

}
