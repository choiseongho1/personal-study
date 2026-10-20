

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
public class StepAllowRestartJobConfig {

    /*
     Step 재실행 허용 실습
     - 기본적으로 Step이 COMPLETED 상태이면 재실행되지 않음
     - allowStartIfComplete(true) 설정으로 다시 실행되도록 설정
    */
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionListener jobLoggerListener;

    @Bean
    public Job stepAllowRestartJob() {
        return new JobBuilder("stepAllowRestartJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(restartableStep())
                .listener(jobLoggerListener)
                .build();
    }

    @Bean
    public Step restartableStep() {
        return new StepBuilder("restartableStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🔁 Step 재실행 테스트 실행됨");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .allowStartIfComplete(false) // ✅ 이미 COMPLETED 상태여도 재실행 허용
                .build();
    }
}
