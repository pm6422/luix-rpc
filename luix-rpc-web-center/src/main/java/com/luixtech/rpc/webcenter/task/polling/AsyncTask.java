package com.luixtech.rpc.webcenter.task.polling;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

@Data
@AllArgsConstructor
public class AsyncTask<T> {
    public static final String                            NAME_STATISTIC = "statistic";
    /**
     * Task ID
     */
    private             String                            id;
    /**
     * Task name
     */
    private             String                            name;
    /**
     * DeferredResult
     */
    private             DeferredResult<ResponseEntity<T>> deferredResult;
}
