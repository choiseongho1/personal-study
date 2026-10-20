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
public class UserNameUpperJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job userNameUpperJob() {
        return new JobBuilder("userNameUpperJob", jobRepository)
                .start(userNameUpperStep())
                .build();
    }

    @Bean
    public Step userNameUpperStep() {
        return new StepBuilder("userNameUpperStep", jobRepository)
                .<User, User>chunk(2, transactionManager) // Chunk 단위로 사용자 처리
                .reader(userReader())                     // DB에서 사용자 목록 조회
                .processor(userProcessor())               // 이름을 대문자로 변환
                .writer(userWriter())                     // DB에 다시 저장
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> userReader() {
        JpaPagingItemReader<User> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT u FROM User u");
        reader.setPageSize(2);
        return reader;
    }

    @Bean
    public ItemProcessor<User, User> userProcessor() {
        return user -> {
            user.setName(user.getName().toUpperCase());
            return user;
        };
    }

    @Bean
    public JpaItemWriter<User> userWriter() {
        JpaItemWriter<User> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
