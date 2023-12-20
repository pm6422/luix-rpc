package com.luixtech.rpc.demoserver.task.polling.queue;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Message {
    private String id;
    private String data;
}
