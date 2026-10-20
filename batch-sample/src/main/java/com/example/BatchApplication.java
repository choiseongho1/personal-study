package com.example;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Scanner;

//@EnableScheduling
@SpringBootApplication
public class BatchApplication {

	public static void main(String[] args) throws Exception {
		// 스프링 컨텍스트 수동 로드
		ApplicationContext context = new SpringApplicationBuilder(BatchApplication.class)
				.web(WebApplicationType.NONE) // 웹 앱 아님
				.run(args);

		JobLauncher jobLauncher = context.getBean(JobLauncher.class);

		// 등록된 Job 목록 조회
		String[] jobBeanNames = context.getBeanNamesForType(Job.class);
		System.out.println("사용 가능한 Job 목록:");
		for (String name : jobBeanNames) {
			System.out.println(" - " + name);
		}

		// 콘솔 입력 받기
		Scanner scanner = new Scanner(System.in);
		System.out.print("실행할 Job 이름을 입력하세요: ");
		String jobName = scanner.nextLine().trim();

		Job job = null;
		try {
			job = context.getBean(jobName, Job.class);
		} catch (Exception e) {
			System.err.println("❌ 입력한 Job 이름이 존재하지 않습니다.");
			System.exit(1);
		}

		// Job 파라미터 생성 후 실행
		JobParameters params = new JobParametersBuilder()
				.addLong("time", System.currentTimeMillis())
				.toJobParameters();

		JobExecution execution = jobLauncher.run(job, params);
		System.out.println("✔ Job 실행 완료. 상태: " + execution.getStatus());

	}
}
