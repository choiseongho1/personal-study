package com.example.config;

import com.example.listener.SimpleJobLoggerListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class LogTaskletJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SimpleJobLoggerListener jobLoggerListener; // ✅ 추가

    @Bean
    public Job logTaskletJob() {
        return new JobBuilder("logTaskletJob", jobRepository)
                .start(logStep())
                .listener(jobLoggerListener) // ✅ 등록!
                .build();
    }

    @Bean
    public Step logStep() {
        return new StepBuilder("logStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("📌 Tasklet 실행됨: " + java.time.LocalDateTime.now());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
