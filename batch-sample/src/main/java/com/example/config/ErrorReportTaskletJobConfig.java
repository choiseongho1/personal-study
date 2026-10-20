package com.example.config;

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

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ErrorReportTaskletJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job errorReportTaskletJob() {
        return new JobBuilder("errorReportTaskletJob", jobRepository)
                .start(errorReportStep())
                .build();
    }

    @Bean
    public Step errorReportStep() {
        return new StepBuilder("errorReportStep", jobRepository)
                .tasklet(errorReportTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet errorReportTasklet() {
        return (contribution, chunkContext) -> {
            String filePath = System.getProperty("java.io.tmpdir") + "/error-report.txt";
            File file = new File(filePath);

            // 1. 에러 메시지 시뮬레이션 목록
            List<String> errors = List.of(
                    "[ERROR] 사용자 ID 1 처리 실패",
                    "[ERROR] 상품 ID 23 재고 부족",
                    "[ERROR] 결제 API 응답 없음",
                    "[TIME] " + LocalDateTime.now()
            );

            // 2. 파일 생성 및 작성
            try (FileWriter writer = new FileWriter(file)) {
                for (String line : errors) {
                    writer.write(line + "\n");
                }
                System.out.println("[생성 완료] " + filePath);
            }

            // 3. 파일 내용 출력
            System.out.println("[파일 내용]");
            Files.readAllLines(file.toPath()).forEach(System.out::println);

            // 4. 파일 삭제
            boolean deleted = file.delete();
            System.out.println(deleted
                    ? "[삭제 완료] " + filePath
                    : "[삭제 실패] " + filePath);

            return RepeatStatus.FINISHED;
        };
    }
}
