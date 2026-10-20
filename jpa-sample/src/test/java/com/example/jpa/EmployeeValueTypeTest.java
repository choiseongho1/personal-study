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
class EmployeeValueTypeTest {

    @Autowired
    EntityManager em;

    @Autowired
    EmployeeRepository employeeRepository;

    @Test
    void employee_값타입_속성재정의_컬렉션테스트() {
        // 📦 값 타입 인스턴스 (공유 X!)
        WorkAddress home = new WorkAddress("서울", "서초대로", "12345");
        WorkAddress work = new WorkAddress("부산", "해운대로", "67890");

        // 🧑‍💼 직원 엔티티 생성
        Employee emp = new Employee("김영희", home, work);
        emp.getHobbies().add("헬스");
        emp.getHobbies().add("독서");

        employeeRepository.save(emp);
        em.flush();
        em.clear();

        // 🔍 다시 조회하여 확인
        Employee found = employeeRepository.findById(emp.getId()).orElseThrow();
        System.out.println("🏠 집 주소: " + found.getHomeAddress().getCity());
        System.out.println("🏢 직장 주소: " + found.getWorkAddress().getCity());
        System.out.println("🎯 취미: " + found.getHobbies());
    }
}
