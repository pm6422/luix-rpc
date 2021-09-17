package org.infinity.luix.demoserver.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

public interface AsyncTaskService {

    void execute(DeferredResult<ResponseEntity<String>> deferred);
}
