package org.infinity.luix.webcenter.task.polling.resulthandler;

import org.infinity.luix.webcenter.task.polling.queue.AsyncTask;

public interface AsyncTaskResultHandler {

    void handle(AsyncTask asyncTask);
}
