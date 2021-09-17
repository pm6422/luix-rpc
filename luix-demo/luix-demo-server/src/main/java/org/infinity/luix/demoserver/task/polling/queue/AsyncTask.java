package org.infinity.luix.demoserver.task.polling.queue;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

@Data
@Builder
public class AsyncTask {

    private String id;

    private DeferredResult<ResponseEntity<String>> deferredResult;
}
