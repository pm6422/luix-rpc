package org.infinity.rpc.common;

import lombok.ToString;

/**
 * Response class of RPC
 */
@Deprecated
@ToString
public class RpcResponse {
    // 响应ID
    private String    responseId;
    // 请求ID
    private String    requestId;
    // 响应是否成功
    private boolean   success;
    // 响应结果
    private Object    result;
    // 如果有异常信息，在该对象中记录异常信息
    private Throwable throwable;

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
