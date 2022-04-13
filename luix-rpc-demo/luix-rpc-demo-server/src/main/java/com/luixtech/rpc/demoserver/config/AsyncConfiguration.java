package com.luixtech.rpc.demoserver.config;

import com.luixtech.rpc.demoserver.async.ExceptionHandlingAsyncTaskExecutor;
import com.luixtech.rpc.demoserver.async.ExceptionHandlingAsyncUncaughtExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer, WebMvcConfigurer {

    @Resource
    private TaskExecutionProperties  taskExecutionProperties;
    @Resource
    private TaskSchedulingProperties taskSchedulingProperties;

    @Override
    @Bean(name = "asyncTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskExecutionProperties.getPool().getCoreSize());
        executor.setMaxPoolSize(taskExecutionProperties.getPool().getMaxSize());
        executor.setQueueCapacity(taskExecutionProperties.getPool().getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());
        log.info("Created async task executor with corePoolSize: [{}], maxPoolSize: [{}] and queueCapacity: [{}]",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), taskExecutionProperties.getPool().getQueueCapacity());
        return new ExceptionHandlingAsyncTaskExecutor(executor);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ExceptionHandlingAsyncUncaughtExceptionHandler();
    }

    /**
     * 暂时搞不清楚作用
     *
     * @return
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(taskSchedulingProperties.getPool().getSize());
        taskScheduler.setRemoveOnCancelPolicy(true);
        taskScheduler.setErrorHandler(t -> log.error("Unexpected error occurred while executing scheduled task!", t));
        taskScheduler.setThreadNamePrefix(taskSchedulingProperties.getThreadNamePrefix());
        return taskScheduler;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor((AsyncTaskExecutor) getAsyncExecutor());
    }
}