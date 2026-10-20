package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ParallelStepJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // ✅ 병렬 Step 실행을 위한 Job 구성
    @Bean
    public Job parallelStepJob() {
        return new JobBuilder("parallelStepJob", jobRepository)
                .start(splitFlow())              // Step1, Step2 병렬 실행
                .next(joinedStep())             // 병렬 이후 Step3 실행
                .end()
                .build();
    }

    // ✅ 병렬로 실행할 Flow 구성
    @Bean
    public Flow splitFlow() {
        Flow flow1 = new FlowBuilder<Flow>("flow1")
                .start(step1())
                .build();

        Flow flow2 = new FlowBuilder<Flow>("flow2")
                .start(step2())
                .build();

        return new FlowBuilder<Flow>("splitFlow")
                .split(taskExecutor())
                .add(flow1, flow2)
                .build();
    }

    // ✅ 병렬 처리용 TaskExecutor (스레드)
    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("parallel-thread-");
    }

    @Bean
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🚀 Step1 시작: 사용자 처리");
                    Thread.sleep(2000); // 일부러 지연
                    System.out.println("✅ Step1 완료");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step2() {
        return new StepBuilder("step2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🚀 Step2 시작: 주문 처리");
                    Thread.sleep(1000); // 일부러 지연
                    System.out.println("✅ Step2 완료");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step joinedStep() {
        return new StepBuilder("step3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("📦 Step3: 리포트 생성");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
