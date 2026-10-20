package com.example.jpa;

import com.example.jpa.entity.Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback(false)
class MappedSuperclassTest {

    @Autowired
    EntityManager em;

    @Test
    void mappedSuperclass_동작_확인() {
        Member member = new Member("홍길동", "Seoul");

        em.persist(member);
        em.flush();
        em.clear();

        Member found = em.find(Member.class, member.getId());
        System.out.println("⏰ createdAt = " + found.getCreatedAt());
        System.out.println("⏰ updatedAt = " + found.getUpdatedAt());
    }
}
