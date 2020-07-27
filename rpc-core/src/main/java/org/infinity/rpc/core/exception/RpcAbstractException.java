/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.rpc.core.exception;

import org.infinity.rpc.core.exchange.request.impl.RequestContext;

public abstract class RpcAbstractException extends RuntimeException {
    private static final long serialVersionUID = -8742311167276890503L;

    protected RpcErrorMsg rpcErrorMsg = RpcErrorMsgConstant.FRAMEWORK_DEFAULT_ERROR;
    protected String      errorMsg    = null;

    public RpcAbstractException() {
        super();
    }

    public RpcAbstractException(RpcErrorMsg rpcErrorMsg) {
        super();
        this.rpcErrorMsg = rpcErrorMsg;
    }

    public RpcAbstractException(String message) {
        super(message);
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, RpcErrorMsg rpcErrorMsg) {
        super(message);
        this.rpcErrorMsg = rpcErrorMsg;
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, Throwable cause) {
        super(message, cause);
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, Throwable cause, RpcErrorMsg rpcErrorMsg) {
        super(message, cause);
        this.rpcErrorMsg = rpcErrorMsg;
        this.errorMsg = message;
    }

    public RpcAbstractException(Throwable cause) {
        super(cause);
    }

    public RpcAbstractException(Throwable cause, RpcErrorMsg rpcErrorMsg) {
        super(cause);
        this.rpcErrorMsg = rpcErrorMsg;
    }

    @Override
    public String getMessage() {
        String message = getOriginMessage();

        return "error_message: " + message + ", status: " + rpcErrorMsg.getStatus() + ", error_code: " + rpcErrorMsg.getErrorCode()
                + ",r=" + RequestContext.getThreadRpcContext().getRequestId();
    }

    public String getOriginMessage() {
        if (rpcErrorMsg == null) {
            return super.getMessage();
        }

        String message;

        if (errorMsg != null && !"".equals(errorMsg)) {
            message = errorMsg;
        } else {
            message = rpcErrorMsg.getMessage();
        }
        return message;
    }

    public int getStatus() {
        return rpcErrorMsg != null ? rpcErrorMsg.getStatus() : 0;
    }

    public int getErrorCode() {
        return rpcErrorMsg != null ? rpcErrorMsg.getErrorCode() : 0;
    }

    public RpcErrorMsg getRpcErrorMsg() {
        return rpcErrorMsg;
    }
}
