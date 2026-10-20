package com.example.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerJobRunner {

    private final JobLauncher jobLauncher;
    private final Job logTaskletJob; // 🔁 실행할 Job (예: logTaskletJob)

    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    public void runJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()) // 중복 방지용 파라미터
                    .toJobParameters();

            System.out.println("🕒 Scheduler 실행 시작");
            JobExecution execution = jobLauncher.run(logTaskletJob, jobParameters);
            System.out.println("✔ 실행 결과: " + execution.getStatus());

        } catch (Exception e) {
            System.out.println("❌ Job 실행 실패: " + e.getMessage());
        }
    }
}
