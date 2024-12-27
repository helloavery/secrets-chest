package com.averygrimes.secretschest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "s3UploadAsyncThreadPoolTaskExecutor")
    public Executor asyncPoolThreadS3BucketExecutor(){
        ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(10);
        threadPoolExecutor.setMaxPoolSize(200);
        threadPoolExecutor.setQueueCapacity(500);
        threadPoolExecutor.setThreadNamePrefix("S3UploadTasksExecutorThread-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }
}