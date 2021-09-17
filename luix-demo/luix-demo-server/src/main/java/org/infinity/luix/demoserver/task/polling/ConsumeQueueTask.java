package org.infinity.luix.demoserver.task.polling;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.demoserver.task.polling.queue.Message;
import org.infinity.luix.demoserver.task.polling.queue.MessageQueue;
import org.infinity.luix.demoserver.task.polling.queue.Task;
import org.infinity.luix.demoserver.task.polling.queue.TaskQueue;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Refer to
 * https://blog.csdn.net/m0_37595562/article/details/81013909
 * https://filia-aleks.medium.com/microservice-performance-battle-spring-mvc-vs-webflux-80d39fd81bf0
 */
@Slf4j
@Component
public class ConsumeQueueTask implements ApplicationRunner {

    @Resource
    private TaskQueue taskQueue;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(this::execute).start();
    }

    private void execute() {
        while (true) {
            try {
                Task task;
                synchronized (taskQueue) {
                    task = taskQueue.take();
                }
                if (task == null) {
                    continue;
                }

                Message message = MessageQueue.get(task.getId());
                if (message != null) {
                    // 根据taskId到redis检索，检索到则设置结果
                    ResponseEntity<String> response = ResponseEntity.ok(message.getData());
                    task.getDeferredResult().setResult(response);
                } else {
                    // 检索不到结果，则重新放入任务队列中
                    taskQueue.put(task);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
