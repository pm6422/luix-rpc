package com.luixtech.rpc.demoserver.controller;

import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.democommon.service.AppService;
import com.luixtech.rpc.demoserver.config.AsyncConfiguration;
import com.luixtech.rpc.demoserver.service.AsyncTaskTestService;
import com.luixtech.rpc.demoserver.task.polling.queue.InMemoryAsyncTaskQueue;
import com.luixtech.rpc.demoserver.task.polling.queue.Message;
import com.luixtech.rpc.demoserver.utils.TraceIdUtils;
import com.luixtech.rpc.spring.boot.starter.config.LuixProperties;
import com.luixtech.uidgenerator.core.id.IdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;

import java.util.concurrent.Callable;

import static com.luixtech.rpc.core.constant.ApplicationConstants.APP;

@RestController
@AllArgsConstructor
@Slf4j
public class TestController {
    private final LuixProperties       luixProperties;
    private final AsyncTaskTestService asyncTaskTestService;

    @Operation(summary = "register provider")
    @GetMapping("/api/tests/register-provider")
    public void registerProvider() {
        Url providerUrl = Url.of(
                luixProperties.getAvailableProtocol().getName(),
                "192.168.0.1",
                luixProperties.getAvailableProtocol().getPort(),
                AppService.class.getName());

        // Assign values to parameters
        providerUrl.addOption(APP, luixProperties.getApplication().getId());

        luixProperties.getRegistryList().forEach(registryConfig -> registryConfig.getRegistryImpl().register(providerUrl));
    }

    @Operation(summary = "blocking response")
    @GetMapping("/api/tests/async/blocking-response")
    public String blockingResponse() {
        log.info("Request received");
        String result = asyncTaskTestService.sendMessage();
        log.info("Servlet thread released");
        return result;
    }

    /**
     * 一次请求使用两个线程，一个servlet请求线程，另外一个是执行线程。执行线程是由Spring使用TaskExecutor来管理线程的。参照
     * {@link AsyncConfiguration#configureAsyncSupport(AsyncSupportConfigurer)}
     * 在等待完成的长期任务之前，servlet请求线程将被释放。
     * <p>
     * 长时间运行的任务执行完毕之前就已经从servlet返回了。这并不意味着客户端收到了一个响应。
     * 与客户端的通信仍然是开放的等待结果，但servlet请求线程已经释放，并可以服务于另一个客户的请求。
     * <p>
     * 现象：Response header里的x-elapsed为0ms，但是Response Body里没有数据，需要等待5s后才可以有结果。
     *
     * @return callable
     */
    @Operation(summary = "callable response")
    @GetMapping("/api/tests/async/callable-response")
    public Callable<String> callableResponse() {
        log.info("Request received");
        Callable<String> callable = asyncTaskTestService::sendMessage;
        log.info("Servlet thread released");
        return callable;
    }

    /**
     * 一次请求使用两个线程，一个servlet请求线程，另外一个是执行线程。执行线程是由自己创建了一个线程并将结果设置到DeferredResult中。
     * 本例是用CompletableFuture创建一个异步任务，这将创建一个新的线程，在那里我们的长时间运行的任务将被执行，并将结果设置到DeferredResult并返回。
     * 是在哪个线程池中我们取回这个新的线程？默认情况下在CompletableFuture的supplyAsync方法将在forkJoin池运行任务。
     * 如果你想使用一个不同的线程池，你可以通过传一个executor到supplyAsync方法：
     * public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor)
     * <p>
     * Callable和DeferredResult相同的事会立即释放servlet请求线程，在另一个线程上异步运行长时间的任务。不同的是谁管理执行任务的线程。
     * <p>
     * 现象：同callableResponse
     *
     * @return deferred result
     */
    @Operation(summary = "deferred result")
    @GetMapping("/api/tests/async/deferred-result/{valid}")
    public DeferredResult<ResponseEntity<String>> sendMessage(@PathVariable boolean valid) {
        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>(5000L);
        handleAsyncError(deferredResult);

        if (!valid) {
            throw new IllegalArgumentException("invalid");
        }

        // Put task in memory queue
        boolean hasCapacity = InMemoryAsyncTaskQueue.offer(TraceIdUtils.getTraceId(), deferredResult);
        if (!hasCapacity) {
            // If the ArrayBlockingQueue is full
            deferredResult.setErrorResult(
                    ResponseEntity.status(HttpStatus.FORBIDDEN).body("Server is busy!"));
        } else {
            // Send message asynchronously
            Message message = Message.builder()
                    .id(TraceIdUtils.getTraceId())
                    .data(String.valueOf(IdGenerator.generateShortId()))
                    .build();
            asyncTaskTestService.sendMessage(message);
        }
        return deferredResult;
    }

    private void handleAsyncError(DeferredResult<ResponseEntity<String>> deferredResult) {
        // Handle timeout
        deferredResult.onTimeout(() ->
                deferredResult.setErrorResult(
                        ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Request timeout!")));
        // Handle error
        deferredResult.onError((Throwable t) -> deferredResult.setErrorResult(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(t.getMessage())));
    }
}
