

package com.example.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class FlatFileUserJobConfig {
    /*
     1단계: CSV → 객체 매핑 (FlatFileItemReader 실습)
     2단계: Processor 추가 (이름 대문자로 변환)
     3단계: DB 저장 추가 (JdbcBatchItemWriter)
     - 회원 정보(users.csv)를 읽어 UserCsv 객체로 매핑
    */
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job flatFileUserJob() {
        return new JobBuilder("flatFileUserJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(readUserCsvStep())
                .build();
    }

    @Bean
    public Step readUserCsvStep() {
        return new StepBuilder("readUserCsvStep", jobRepository)
                .<UserCsv, UserCsv>chunk(5, transactionManager)
                .reader(userCsvReader())
                .processor(upperCaseProcessor())
                .writer(userJdbcWriter())
                .build();
    }

    @Bean
    public FlatFileItemReader<UserCsv> userCsvReader() {
        return new FlatFileItemReaderBuilder<UserCsv>()
                .name("userCsvReader")
                .resource(new ClassPathResource("input/users.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "name", "email")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(UserCsv.class);
                }})
                .build();
    }

    @Bean
    public ItemProcessor<UserCsv, UserCsv> upperCaseProcessor() {
        return user -> {
            user.setName(user.getName().toUpperCase());
            return user;
        };
    }

    @Bean
    public JdbcBatchItemWriter<UserCsv> userJdbcWriter() {
        JdbcBatchItemWriter<UserCsv> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO app_user (id, name) VALUES (:id, :name)");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.afterPropertiesSet();
        return writer;
    }

    @Data
    public static class UserCsv {
        private Long id;
        private String name;
        private String email;
    }
}
