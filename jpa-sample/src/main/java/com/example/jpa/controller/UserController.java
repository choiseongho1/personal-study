package com.example.jpa.controller;

import com.example.jpa.dto.OrderDto;
import com.example.jpa.dto.UserCustomDto;
import com.example.jpa.dto.UserDto;
import com.example.jpa.dto.UserSearchDto;
import com.example.jpa.entity.User;
import com.example.jpa.repository.OrderRepository;
import com.example.jpa.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<UserDto> findAll() {

        return userRepository.findAll().stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getOrders().stream()
                    .map(order -> new OrderDto(
                        order.getId(),
                        order.getOrderDate(),
                        null // 순환 방지
                    ))
                    .toList()
            ))
            .toList();
    }

    @GetMapping("/search")
    public List<UserDto> searchUsers(@RequestParam String name) {
        return userRepository.findByNameWithOrders(name).stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getOrders().stream()
                    .map(order -> new OrderDto(
                        order.getId(),
                        order.getOrderDate(),
                        null
                    ))
                    .toList()
            ))
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(user -> new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getOrders().stream()
                    .map(order -> new OrderDto(
                        order.getId(),
                        order.getOrderDate(),
                        null
                    ))
                    .toList()
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/fetch-users")
    public List<UserDto> findAllUsersFetchJoin() {
        return userRepository.findAllWithOrders().stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getOrders().stream()
                    .map(order -> new OrderDto(
                        order.getId(),
                        order.getOrderDate(),
                        null
                    ))
                    .toList()
            ))
            .toList();
    }

    @GetMapping("/fetch-users/search")
    public List<UserDto> searchUsersFetchJoin(@RequestParam String name) {
        return userRepository.findByNameWithOrdersFetchJoin(name).stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getOrders().stream()
                    .map(order -> new OrderDto(
                        order.getId(),
                        order.getOrderDate(),
                        null
                    ))
                    .toList()
            ))
            .toList();
    }


    @GetMapping("/querydsl")
    public List<UserCustomDto> searchUsersQuerydsl() {
        return userRepository.findAllUserDtos();
    }

    @GetMapping("/nestedDto1")
    public List<UserCustomDto> searchUsersNestedDto1() {
        return userRepository.findAllUserDtosNestedDtoType1();
    }

    @GetMapping("/nestedDto2")
    public List<UserCustomDto> searchUsersNestedDto2() {
        return userRepository.findAllUserDtosNestedDtoType2();
    }

    @GetMapping("/searchUsers")
    public List<UserSearchDto> searchUsers(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email
    ) {
        return userRepository.searchUsers(name, email);
    }

}
