package com.luixtech.luixrpc.demoserver.task.polling.queue;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

@Data
@Builder
public class AsyncTask {
    /**
     * Task ID
     */
    private String                                 id;
    /**
     * DeferredResult
     */
    private DeferredResult<ResponseEntity<String>> deferredResult;
}
