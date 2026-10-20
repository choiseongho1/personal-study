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
public class StepExecutionSharingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // ExecutionContext에 저장할 key 이름 상수화
    private static final String CONTEXT_KEY = "filePath";

    // 전체 Job 구성: 3개의 Step을 순차 실행
    @Bean
    public Job stepSharingJob() {
        return new JobBuilder("stepSharingJob", jobRepository)
                .start(generateStep())
                .next(printStep())
                .next(deleteStep())
                .build();
    }

    // Step1: 에러 로그 파일 생성 및 경로 저장
    @Bean
    public Step generateStep() {
        return new StepBuilder("generateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String path = System.getProperty("java.io.tmpdir") + "/shared-error-report.txt";
                    List<String> lines = List.of(
                            "[ERROR] 쿠폰 적용 실패",
                            "[ERROR] 배송 지연",
                            "[TIME] " + LocalDateTime.now()
                    );

                    try (FileWriter writer = new FileWriter(path)) {
                        for (String line : lines) {
                            writer.write(line + "\n");
                        }
                        System.out.println("[Step1] 파일 생성 완료: " + path);
                    }

                    // JobExecutionContext에 경로 저장 (다음 Step에서 사용)
                    StepExecution stepExecution = contribution.getStepExecution();
                    stepExecution.getJobExecution().getExecutionContext().putString(CONTEXT_KEY, path);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // Step2: 저장된 경로를 기반으로 파일 읽기
    @Bean
    public Step printStep() {
        return new StepBuilder("printStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String path = contribution.getStepExecution()
                            .getJobExecution()
                            .getExecutionContext()
                            .getString(CONTEXT_KEY);

                    System.out.println("[Step2] 파일 내용 ↓↓↓");
                    Files.readAllLines(new File(path).toPath())
                            .forEach(System.out::println);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // Step3: 동일한 경로로 파일 삭제 수행
    @Bean
    public Step deleteStep() {
        return new StepBuilder("deleteStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String path = contribution.getStepExecution()
                            .getJobExecution()
                            .getExecutionContext()
                            .getString(CONTEXT_KEY);

                    boolean deleted = new File(path).delete();
                    System.out.println(deleted
                            ? "[Step3] 파일 삭제 완료"
                            : "[Step3] 파일 삭제 실패");

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
