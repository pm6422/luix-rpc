package com.luixtech.luixrpc.demoserver.service;

import com.luixtech.luixrpc.demoserver.task.polling.queue.Message;

public interface AsyncTaskTestService {

    String sendMessage();

    void sendMessage(Message message);
}
