package com.example.jpa.entity;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class WorkAddress {

    private String city;
    private String street;
    private String zipCode;

    protected WorkAddress() {}

    public WorkAddress(String city, String street, String zipCode) {
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
    }


    // equals & hashCode (중요!)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkAddress)) return false;
        WorkAddress that = (WorkAddress) o;
        return Objects.equals(city, that.city) &&
            Objects.equals(street, that.street) &&
            Objects.equals(zipCode, that.zipCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, street, zipCode);
    }
}
