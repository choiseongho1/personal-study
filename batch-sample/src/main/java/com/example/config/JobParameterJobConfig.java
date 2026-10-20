package com.example.config;

import com.example.entity.User;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class JobParameterJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job jobParameterJob() {
        return new JobBuilder("jobParameterJob", jobRepository)
                .start(jobParameterStep())
                .build();
    }

    @Bean
    public Step jobParameterStep() {
        return new StepBuilder("jobParameterStep", jobRepository)
                .<User, User>chunk(2, transactionManager)
                .reader(parameterReader())
                .processor(stepScopeProcessor(null)) // 주입은 StepScope에서 처리
                .writer(parameterWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> parameterReader() {
        JpaPagingItemReader<User> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT u FROM User u");
        reader.setPageSize(2);
        return reader;
    }

    // 파라미터를 사용하는 Processor
    @Bean
    @StepScope
    public ItemProcessor<User, User> stepScopeProcessor(@Value("#{jobParameters['date']}") String date) {
        return user -> {
            System.out.println("📅 실행 파라미터(date): " + date);
            user.setName(user.getName() + "_" + date.replace("-", ""));
            return user;
        };
    }

    @Bean
    public JpaItemWriter<User> parameterWriter() {
        JpaItemWriter<User> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
