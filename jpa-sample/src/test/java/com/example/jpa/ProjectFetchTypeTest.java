package com.example.jpa;

import com.example.jpa.entity.Project;
import com.example.jpa.entity.Task;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback(false)
class ProjectFetchTypeTest {

    @Autowired
    EntityManager em;

    @Test
    void fetchType_비교_실험() {
        // ✅ 데이터 저장
        Project project = new Project("AI 서비스 개발");
        em.persist(project);

        for (int i = 1; i <= 3; i++) {
            em.persist(new Task("업무 " + i, project));
        }

        em.flush();
        em.clear();

        // ✅ 테스트 시작 (쿼리 로그 주의 깊게 보기)
        System.out.println("▶ Project 조회 (tasks는 아직 안 봄)");
        Project found = em.find(Project.class, project.getId());

        System.out.println("▶ tasks에 접근 시점");
        for (Task task : found.getTasks()) {
            System.out.println("📝 " + task.getTitle());
        }
    }
}
