package com.medkernel.shared.observability;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 异步任务执行器配置。
 *
 * <p>启用 @EnableAsync；注入 {@link TraceIdPropagator#wrap(Runnable)} 作为
 * TaskDecorator，确保所有 @Async / @Scheduled 自动传递 RequestContext + MDC。
 */
@Configuration
@EnableAsync
public class AsyncTaskExecutorConfig {

    @Bean
    public TaskDecorator traceTaskDecorator() {
        return TraceIdPropagator::wrap;
    }

    @Bean(name = "applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor(TaskDecorator traceTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("medkernel-async-");
        executor.setTaskDecorator(traceTaskDecorator);
        executor.initialize();
        return executor;
    }
}
