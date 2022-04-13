package com.luixtech.luixrpc.webcenter.task.polling.resulthandler;

import com.luixtech.luixrpc.webcenter.task.polling.AsyncTask;

public interface AsyncTaskResultHandler {

    void handleResult(AsyncTask<?> asyncTask);
}
