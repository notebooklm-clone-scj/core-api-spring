package core_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "documentTaskExecutor")
    public Executor documentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);    // 기본적으로 대기하는 스레드 수
        executor.setMaxPoolSize(10);    // 바쁠 때 최대로 늘릴 스레드 수
        executor.setQueueCapacity(50);  // 10개가 다 일하고 있으면 대기열에 50개까지 줄 세움
        executor.setThreadNamePrefix("DocAsync-"); // 로그 볼 때 스레드 이름표
        executor.initialize();
        return executor;
    }
}
