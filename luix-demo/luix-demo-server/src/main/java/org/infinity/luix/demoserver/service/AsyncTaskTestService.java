package org.infinity.luix.demoserver.service;

import org.infinity.luix.demoserver.task.polling.queue.Message;

public interface AsyncTaskTestService {

    String sendMessage();

    void sendMessage(Message message);
}
