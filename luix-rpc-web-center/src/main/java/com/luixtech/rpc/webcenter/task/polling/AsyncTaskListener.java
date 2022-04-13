package com.luixtech.rpc.webcenter.task.polling;

import com.luixtech.rpc.webcenter.task.polling.queue.InMemoryAsyncTaskQueue;
import com.luixtech.rpc.webcenter.task.polling.resulthandler.AsyncTaskResultHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Indefinite polling from {@link InMemoryAsyncTaskQueue}
 * used to set result to {@link org.springframework.web.context.request.async.DeferredResult} if there are completed async task
 * <p>
 * Refer to
 * https://blog.csdn.net/m0_37595562/article/details/81013909
 * https://filia-aleks.medium.com/microservice-performance-battle-spring-mvc-vs-webflux-80d39fd81bf0
 */
@Slf4j
@Component
public class AsyncTaskListener implements ApplicationRunner {

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(this::execute).start();
    }

    private void execute() {
        while (true) {
            try {
                AsyncTask<?> asyncTask = InMemoryAsyncTaskQueue.poll();
                if (asyncTask == null) {
                    // Sleep for a while in order to decrease CPU occupation, otherwise the CPU occupation will reach to 100%
                    Thread.sleep(100L);
                    continue;
                }
                Objects.requireNonNull(getResultHandler(asyncTask)).handleResult(asyncTask);
            } catch (Exception e) {
                log.error("Failed to consume async task!", e);
            }
        }
    }

    private AsyncTaskResultHandler getResultHandler(AsyncTask<?> asyncTask) {
        return applicationContext.getBean(
                asyncTask.getName().concat(AsyncTaskResultHandler.class.getSimpleName()),
                AsyncTaskResultHandler.class);
    }
}
