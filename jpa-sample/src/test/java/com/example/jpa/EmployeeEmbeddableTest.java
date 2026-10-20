package com.example.jpa;

import com.example.jpa.entity.Employee;
import com.example.jpa.entity.WorkAddress;
import com.example.jpa.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback(false)
class EmployeeEmbeddableTest {

    @Autowired
    EntityManager em;
    @Autowired
    EmployeeRepository employeeRepository;

    @Test
    void 값타입_기본_동작_테스트() {
//        WorkAddress address1 = new WorkAddress("Seoul", "강남대로", "12345");
//        Employee emp1 = new Employee("철수", address1);
//        Employee emp2 = new Employee("영희", address1); // ❗ 동일한 주소 인스턴스 사용
//
//        employeeRepository.save(emp1);
//        employeeRepository.save(emp2);
//        em.flush();
//
//        System.out.println("▶ 영희의 주소 변경");
//        emp2.setAddress(new WorkAddress("Busan", "해운대", "67890"));
//        em.flush(); // dirty checking
//
//        System.out.println("▶ 철수의 주소: " + emp1.getAddress().getCity());
    }
}
