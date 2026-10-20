package com.example.jpa.controller;

import com.example.jpa.dto.OrderDto;
import com.example.jpa.dto.UserDto;
import com.example.jpa.entity.Order;
import com.example.jpa.entity.User;
import com.example.jpa.repository.OrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    @GetMapping
    public List<OrderDto> findAll() {
        return orderRepository.findAll().stream()
            .map(order -> {
                User user = order.getUser();
                UserDto userDto = new UserDto(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    List.of() // 무한 루프 방지: 비워두기
                );
                return new OrderDto(
                    order.getId(),
                    order.getOrderDate(),
                    userDto
                );
            })
            .toList();
    }
}
