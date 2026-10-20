package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;


@Configuration
@RequiredArgsConstructor
public class MultiStepErrorJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private static final String FILE_PATH =
            System.getProperty("java.io.tmpdir") + "/multi-step-error-report.txt";

    // 전체 Job 구성
    @Bean
    public Job multiStepErrorJob() {
        return new JobBuilder("multiStepErrorJob", jobRepository)
                .start(generateErrorReportStep()) // Step1: 파일 생성
                .next(printErrorReportStep())     // Step2: 내용 출력
                .next(deleteErrorReportStep())    // Step3: 파일 삭제
                .build();
    }

    // Step1: 파일 생성 Tasklet
    @Bean
    public Step generateErrorReportStep() {
        return new StepBuilder("generateErrorReportStep", jobRepository)
                .tasklet(generateTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet generateTasklet() {
        return (contribution, chunkContext) -> {
            List<String> errors = List.of(
                    "[ERROR] 주문 ID 12 처리 실패",
                    "[ERROR] 결제 실패",
                    "[ERROR] 이메일 발송 오류",
                    "[TIME] " + LocalDateTime.now()
            );

            try (FileWriter writer = new FileWriter(FILE_PATH)) {
                for (String line : errors) {
                    writer.write(line + "\n");
                }
                System.out.println("[Step1] 에러 리포트 생성 완료");
            }

            return RepeatStatus.FINISHED;
        };
    }

    // Step2: 파일 읽기 Tasklet
    @Bean
    public Step printErrorReportStep() {
        return new StepBuilder("printErrorReportStep", jobRepository)
                .tasklet(printTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet printTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("[Step2] 리포트 내용 ↓↓↓");
            Files.readAllLines(new File(FILE_PATH).toPath())
                    .forEach(System.out::println);
            return RepeatStatus.FINISHED;
        };
    }

    // Step3: 파일 삭제 Tasklet
    @Bean
    public Step deleteErrorReportStep() {
        return new StepBuilder("deleteErrorReportStep", jobRepository)
                .tasklet(deleteTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet deleteTasklet() {
        return (contribution, chunkContext) -> {
            boolean deleted = new File(FILE_PATH).delete();
            System.out.println(deleted
                    ? "[Step3] 리포트 파일 삭제 완료"
                    : "[Step3] 리포트 파일 삭제 실패");
            return RepeatStatus.FINISHED;
        };
    }
}
