package org.infinity.luix.demoserver.service;

import org.infinity.luix.demoserver.task.polling.queue.Message;

public interface AsyncTaskService {

    String sendMessage();

    void sendMessage(Message message);
}
