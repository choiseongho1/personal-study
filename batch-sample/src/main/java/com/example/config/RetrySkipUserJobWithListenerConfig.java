package com.example.config;

import com.example.entity.User;
import com.example.listener.CustomRetryListener;
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

/**
 * ✅ Retry + Skip + Listener 구성
 * - 이름이 'ERROR'인 유저에서 예외 발생
 * - Retry 3회 후 Skip
 * - Listener 통해 Retry 로그 확인
 */
@Configuration
@RequiredArgsConstructor
public class RetrySkipUserJobWithListenerConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final CustomRetryListener customRetryListener; // ✅ 기존 Listener 주입

    @Bean
    public Job retrySkipUserJobWithListener() {
        return new JobBuilder("retrySkipUserJobWithListener", jobRepository)
                .start(retryStepWithListener())
                .build();
    }

    @Bean
    public Step retryStepWithListener() {
        return new StepBuilder("retryStepWithListener", jobRepository)
                .<User, User>chunk(2, transactionManager)
                .reader(retryListenerUserReader())
                .processor(retryListenerProcessor())
                .writer(retryListenerUserWriter())
                .faultTolerant()
                .retry(RuntimeException.class)
                .retryLimit(3)
                .skip(RuntimeException.class)
                .skipLimit(5)
                .listener(customRetryListener) // ✅ Bean 직접 주입하여 등록
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> retryListenerUserReader() {
        JpaPagingItemReader<User> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT u FROM User u");
        reader.setPageSize(2);
        return reader;
    }

    @Bean
    public ItemProcessor<User, User> retryListenerProcessor() {
        return user -> {
            if ("ERROR".equalsIgnoreCase(user.getName())) {
                System.out.println("❗ 예외 유도: 사용자 이름이 ERROR");
                throw new RuntimeException("의도된 처리 오류");
            }
            user.setName(user.getName().toUpperCase());
            return user;
        };
    }

    @Bean
    public JpaItemWriter<User> retryListenerUserWriter() {
        JpaItemWriter<User> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
