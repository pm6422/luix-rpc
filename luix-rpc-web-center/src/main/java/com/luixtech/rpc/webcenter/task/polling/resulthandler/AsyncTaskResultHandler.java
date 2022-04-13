package com.luixtech.rpc.webcenter.task.polling.resulthandler;

import com.luixtech.rpc.webcenter.task.polling.AsyncTask;

public interface AsyncTaskResultHandler {

    void handleResult(AsyncTask<?> asyncTask);
}
