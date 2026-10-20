package com.example.jpa;

import com.example.jpa.entity.Member;
import com.example.jpa.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberPersistenceTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void dirtyCheckingTest() {
        // 1. 저장
        Member member = new Member("홍길동", "Seoul");
        memberRepository.save(member);

        // 2. 1차 캐시에서 조회됨 (쿼리 X)
        Member found = memberRepository.findById(member.getId()).orElseThrow();

        // 3. 값 변경 → persist 호출 없이 update 발생
        found.setName("김길동");

        // 4. flush 시점에 update 쿼리 실행됨
        em.flush();
    }
}
