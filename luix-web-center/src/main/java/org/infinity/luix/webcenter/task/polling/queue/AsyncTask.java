package org.infinity.luix.webcenter.task.polling.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

@Data
@AllArgsConstructor
public class AsyncTask<T> {
    /**
     * Task ID
     */
    private String                            id;
    /**
     * Task name
     */
    private String                            name;
    /**
     * DeferredResult
     */
    private DeferredResult<ResponseEntity<T>> deferredResult;
}
