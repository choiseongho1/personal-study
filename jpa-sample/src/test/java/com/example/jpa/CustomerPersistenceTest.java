package com.example.jpa;

import com.example.jpa.entity.Customer;
import com.example.jpa.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback(false)
class CustomerPersistenceTest {

    @Autowired
    EntityManager em;
    @Autowired
    CustomerRepository customerRepository;

    @Test
    void 영속성컨텍스트_상태변화_테스트() {
        System.out.println("▶ 엔티티 저장");
        Customer customer = new Customer("홍길동", "010-1234-5678");
        customerRepository.save(customer);

        System.out.println("▶ flush 호출 (DB 반영)");
        em.flush(); // INSERT 쿼리 발생

        System.out.println("▶ clear 호출 (1차 캐시 비움)");
        em.clear(); // customer는 이제 준영속 상태

        System.out.println("▶ findById 재조회 → 새로운 영속 객체");
        Customer found = customerRepository.findById(customer.getId()).orElseThrow();
        System.out.println("📦 조회된 이름: " + found.getName());

        System.out.println("▶ detach 호출 → 특정 엔티티 준영속 상태로 만듦");
        em.detach(found);

        found.setName("김길동"); // 이 변경은 반영 안 됨
        em.flush(); // update 안 나감!

        System.out.println("▶ merge 호출 → 다시 영속 상태로");
        Customer merged = em.merge(found);
        merged.setName("이길동"); // 이 변경은 반영됨

        em.flush(); // update 쿼리 발생

        System.out.println(merged.getName());
    }
}
