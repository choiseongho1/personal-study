package com.example.jpa.repository;

import static com.querydsl.core.group.GroupBy.groupBy;

import com.example.jpa.dto.OrderCustomDto;
import com.example.jpa.dto.QOrderCustomDto;
import com.example.jpa.dto.QUserCustomDto;
import com.example.jpa.dto.UserCustomDto;
import com.example.jpa.dto.UserSearchDto;
import com.example.jpa.entity.QOrder;
import com.example.jpa.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserCustomDto> findAllUserDtos() {
        QUser user = QUser.user;

        return queryFactory
            .select(new QUserCustomDto(user.id, user.name, user.email))
            .from(user)
            .fetch();
    }


    @Override
    public List<UserCustomDto> findAllUserDtosNestedDtoType1() {
        QUser user = QUser.user;
        QOrder order = QOrder.order;

        // 1. User 기본 정보 조회
        List<UserCustomDto> users = queryFactory
            .select(new QUserCustomDto(user.id, user.name, user.email))
            .from(user)
            .fetch();

        // 2. Order 정보 조회 (userId 포함)
        Map<Long, List<OrderCustomDto>> orderMap = queryFactory
            .select(new QOrderCustomDto(order.id, order.orderDate, order.user.id))
            .from(order)
            .where(order.user.id.in(users.stream().map(UserCustomDto::getId).toList()))
            .fetch()
            .stream()
            .collect(Collectors.groupingBy(OrderCustomDto::getUserId));

        // 3. 유저에 주문 내역 조립
        users.forEach(userDto ->
            userDto.setOrders(orderMap.getOrDefault(userDto.getId(), List.of()))
        );

        return users;
    }

    public List<UserCustomDto> findAllUserDtosNestedDtoType2() {
        QUser user = QUser.user;
        QOrder order = QOrder.order;

        return queryFactory
            .from(user)
            .leftJoin(user.orders, order)
            .transform(
                groupBy(user.id).list(
                    new QUserCustomDto(
                        user.id,
                        user.name,
                        user.email,
                        GroupBy.list(
                            new QOrderCustomDto(order.id, order.orderDate, user.id)
                        )
                    )
                )
            );
    }


    @Override
    public List<UserSearchDto> searchUsers(String name, String email) {
        QUser user = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();
        if (name != null) {
            builder.and(user.name.containsIgnoreCase(name));
        }
        if (email != null) {
            builder.and(user.email.containsIgnoreCase(email));
        }

        return queryFactory
            .select(Projections.constructor(UserSearchDto.class,
                user.id,
                user.name,
                user.email))
            .from(user)
            .where(builder)
            .fetch();
    }
}
