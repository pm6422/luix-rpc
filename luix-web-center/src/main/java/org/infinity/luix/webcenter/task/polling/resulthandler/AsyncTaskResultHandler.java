package org.infinity.luix.webcenter.task.polling.resulthandler;

import org.infinity.luix.webcenter.task.polling.AsyncTask;

public interface AsyncTaskResultHandler {

    void handleResult(AsyncTask<?> asyncTask);
}
