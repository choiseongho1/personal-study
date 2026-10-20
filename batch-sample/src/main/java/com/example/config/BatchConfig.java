package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Job 정의 - 하나의 Step을 가지는 simpleJob
    @Bean
    public Job simpleJob() {
        return new JobBuilder("simpleJob", jobRepository)
                .start(simpleStep())
                .build();
    }

    // Step 정의 - 메모리 리스트를 읽어 대문자로 변환 후 출력
    @Bean
    public Step simpleStep() {
        return new StepBuilder("simpleStep", jobRepository)
                .<String, String>chunk(3, transactionManager)
                .reader(itemReader())      // Reader: 문자열 리스트
                .processor(itemProcessor()) // Processor: 대문자 변환
                .writer(itemWriter())      // Writer: 콘솔 출력
                .build();
    }

    @Bean
    public ItemReader<String> itemReader() {
        return new ListItemReader<>(List.of("spring", "batch", "update"));
    }

    @Bean
    public ItemProcessor<String, String> itemProcessor() {
        return item -> item.toUpperCase();
    }

    @Bean
    public ItemWriter<String> itemWriter() {
        return items -> items.forEach(System.out::println);
    }
}
