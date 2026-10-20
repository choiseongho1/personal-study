package com.example.jpa.dto;

import java.time.LocalDateTime;

// src/main/java/com/example/jpa/dto/OrderDto.java
public record OrderDto(Long id, LocalDateTime orderDate, UserDto user) {}
