
package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class ConditionalFailJobConfig {

    /*
     ExitStatus 직접 설정 실습
     - Step은 정상 종료되지만, 조건에 따라 FAILED 상태로 마킹하여 Job 전체를 실패로 처리
    */
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
//    private final JobExecutionListener jobLoggerListener;

    @Bean
    public Job conditionalFailJob() {
        return new JobBuilder("conditionalFailJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(checkBusinessRuleStep())
//                .listener(jobLoggerListener)
                .build();
    }

    @Bean
    public Step checkBusinessRuleStep() {
        return new StepBuilder("checkBusinessRuleStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int day = LocalDate.now().getDayOfMonth();
                    System.out.println("📅 오늘 날짜: " + day);

                    if (day >= 10) {
                        System.out.println("❌ 조건 위반 → FAILED 처리");
                        contribution.setExitStatus(ExitStatus.FAILED);
                        throw new JobExecutionException("조건 위반으로 Job 중단");
                    } else {
                        System.out.println("✅ 조건 만족 → 정상 종료");
                    }

                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
