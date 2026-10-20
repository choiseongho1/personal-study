

package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class JobExecutionDeciderJobConfig {

    /*
    JobExecutionDecider 실습
    - 현재 시간의 초(second)가 짝수인지 홀수인지에 따라 분기 실행
    - Step1 → Decider → 짝수면 stepEven, 홀수면 stepOdd → 공통 stepFinal 실행
    */

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 전체 Job 구성
    @Bean
    public Job jobExecutionDeciderJob() {
        return new JobBuilder("jobExecutionDeciderJob", jobRepository)
                .start(jobExecutionDeciderStep1())  // Step1 실행 후
                .next(evenOddDecider())             // Decider 로직 수행
                    .on("EVEN").to(stepEven())      // 짝수면 stepEven 실행
                .from(evenOddDecider())
                    .on("ODD").to(stepOdd())        // 홀수면 stepOdd 실행
                .from(stepEven())
                    .on("*").to(stepFinal())         // 이후 공통 Step 실행
                .from(stepOdd())
                    .on("*").to(stepFinal())
                .end()
                .build();
    }

    // 전처리 Step
    @Bean
    public Step jobExecutionDeciderStep1() {
        return new StepBuilder("jobExecutionDeciderStep1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🚀 Step1: 전처리 실행");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // 짝수 분기 Step
    @Bean
    public Step stepEven() {
        return new StepBuilder("stepEven", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("짝수 분기 실행됨 (EVEN)");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // 홀수 분기 Step
    @Bean
    public Step stepOdd() {
        return new StepBuilder("stepOdd", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("홀수 분기 실행됨 (ODD)");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // 마지막 공통 Step
    @Bean
    public Step stepFinal() {
        return new StepBuilder("stepFinal", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🏁 마지막 Step 실행 (공통)");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // Decider: 현재 초가 짝수면 EVEN, 홀수면 ODD 반환
    @Bean
    public JobExecutionDecider evenOddDecider() {
        return (jobExecution, stepExecution) -> {
            int second = LocalDateTime.now().getSecond();
            String result = (second % 2 == 0) ? "EVEN" : "ODD";
            System.out.println("⏱ 현재 초: " + second + " → 분기 결과: " + result);
            return new FlowExecutionStatus(result);
        };
    }
}
