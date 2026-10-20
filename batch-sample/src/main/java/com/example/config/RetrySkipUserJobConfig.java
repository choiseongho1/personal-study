package com.example.config;

import com.example.entity.User;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class RetrySkipUserJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job retrySkipUserJob() {
        return new JobBuilder("retrySkipUserJob", jobRepository)
                .start(retrySkipUserStep())
                .build();
    }

    @Bean
    public Step retrySkipUserStep() {
        return new StepBuilder("retrySkipUserStep", jobRepository)
                .<User, User>chunk(2, transactionManager) // 2개씩 Chunk 처리
                .reader(retryUserReader())                // 사용자 목록 조회
                .processor(retryProcessor())              // 예외 유도 + 대문자 변환
                .writer(retryUserWriter())                // DB에 저장
                .faultTolerant()                          // 예외 허용 모드 시작
                .retry(RuntimeException.class)            // RuntimeException 발생 시 재시도
                .retryLimit(3)                            // 최대 3번까지 재시도
                .skip(RuntimeException.class)             // 그래도 실패하면 Skip
                .skipLimit(5)                             // 최대 5건까지 Skip 허용
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> retryUserReader() {
        JpaPagingItemReader<User> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT u FROM User u");
        reader.setPageSize(2);
        return reader;
    }

    @Bean
    public ItemProcessor<User, User> retryProcessor() {
        return user -> {
            if ("ERROR".equalsIgnoreCase(user.getName())) {
                System.out.println("⚠예외 유도: 사용자 이름이 ERROR");
                throw new RuntimeException("의도적 에러");
            }
            user.setName(user.getName().toUpperCase());
            return user;
        };
    }

    @Bean
    public JpaItemWriter<User> retryUserWriter() {
        JpaItemWriter<User> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
