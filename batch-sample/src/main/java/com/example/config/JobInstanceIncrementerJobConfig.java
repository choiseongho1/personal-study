package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class JobInstanceIncrementerJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionListener jobLoggerListener;

    @Bean
    public Job jobInstanceIncrementerJob() {
        return new JobBuilder("jobInstanceIncrementerJob", jobRepository)
                .incrementer(new RunIdIncrementer()) // ✅ JobInstance를 매번 새로 생성
                .start(incrementerStep())
                .listener(jobLoggerListener)
                .build();
    }

    @Bean
    public Step incrementerStep() {
        return new StepBuilder("incrementerStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🌀 Incrementer 기반 Step 실행됨");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
