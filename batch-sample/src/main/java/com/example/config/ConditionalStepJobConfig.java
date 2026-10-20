package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ConditionalStepJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private boolean shouldFail = false; // ✅ 예외 유도 플래그 (true면 실패)

    @Bean
    public Job conditionalStepJob() {
        return new JobBuilder("conditionalStepJob", jobRepository)
                .start(conditionalStep1())
                    .on("COMPLETED").to(conditionalStep2())       // Step1 성공 시 → Step2
                .from(conditionalStep1())
                    .on("*").to(conditionalStep3())               // Step1 실패 등 → Step3
                .from(conditionalStep2())
                    .on("*").to(conditionalStep3())               // Step2 완료 시 → Step3
                .end()
                .build();
    }

    @Bean
    public Step conditionalStep1() {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🚀 Step1 실행");
                    if (shouldFail) {
                        System.out.println("❌ Step1 실패 유도!");
                        throw new RuntimeException("실패 테스트");
                    }
                    System.out.println("✅ Step1 성공");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step conditionalStep2() {
        return new StepBuilder("step2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("📦 Step2 실행");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step conditionalStep3() {
        return new StepBuilder("step3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("📦 Step3 실행 (마무리 단계)");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
