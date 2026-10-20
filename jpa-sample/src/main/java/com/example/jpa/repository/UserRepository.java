package com.example.jpa.repository;

import com.example.jpa.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    // 기본 EntityGraph
    @EntityGraph(attributePaths = {"orders"})
    List<User> findAll();

    // 사용자 이름으로 조회 + orders fetch
    @EntityGraph(attributePaths = {"orders"})
    @Query("select u from User u where u.name = :name")
    List<User> findByNameWithOrders(@Param("name") String name);

    // ID 기반 단건 조회 (Optional로)
    @EntityGraph(attributePaths = {"orders"})
    Optional<User> findById(Long id);

    // User + Order를 fetch join
    @Query("select u from User u left join fetch u.orders")
    List<User> findAllWithOrders();

    // 조건 + fetch join (name으로 검색)
    @Query("select u from User u left join fetch u.orders where u.name = :name")
    List<User> findByNameWithOrdersFetchJoin(@Param("name") String name);
}
