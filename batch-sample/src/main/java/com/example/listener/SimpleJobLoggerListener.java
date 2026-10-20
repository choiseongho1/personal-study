package com.example.listener;


import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class SimpleJobLoggerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("🟡 [Before Job] " + jobExecution.getJobInstance().getJobName());
        System.out.println("  ⏰ 시작 시간: " + jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.println("🟢 [After Job] " + jobExecution.getJobInstance().getJobName());
        System.out.println("  ✅ 종료 상태: " + jobExecution.getStatus());
        System.out.println("  ⏱ 종료 시간: " + jobExecution.getEndTime());
    }
}
