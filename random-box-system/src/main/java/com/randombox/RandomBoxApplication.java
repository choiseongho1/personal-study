package com.randombox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RandomBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(RandomBoxApplication.class, args);
    }
}
