package com.luixtech.rpc.demoserver.service;

import com.luixtech.rpc.demoserver.task.polling.queue.Message;

public interface AsyncTaskTestService {

    String sendMessage();

    void sendMessage(Message message);
}
