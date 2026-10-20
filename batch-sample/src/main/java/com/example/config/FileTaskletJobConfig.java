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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class FileTaskletJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job fileTaskletJob() {
        return new JobBuilder("fileTaskletJob", jobRepository)
                .start(fileStep())
                .build();
    }

    @Bean
    public Step fileStep() {
        return new StepBuilder("fileStep", jobRepository)
                .tasklet(fileTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet fileTasklet() {
        return (contribution, chunkContext) -> {
            String filePath = System.getProperty("java.io.tmpdir") + "/report.txt";
            File file = new File(filePath);

            // 1. 파일 생성 및 내용 작성
            try (FileWriter writer = new FileWriter(file)) {
                String content = "📄 리포트 생성 시간: " + LocalDateTime.now();
                writer.write(content);
                System.out.println("[생성 완료] " + filePath);
                System.out.println("[내용] " + content);
            } catch (IOException e) {
                throw new RuntimeException("파일 생성 실패", e);
            }

            // 2. 파일 삭제
            boolean deleted = file.delete();
            System.out.println(deleted
                    ? "[삭제 완료] " + filePath
                    : "[삭제 실패] " + filePath);

            return RepeatStatus.FINISHED;
        };
    }
}
