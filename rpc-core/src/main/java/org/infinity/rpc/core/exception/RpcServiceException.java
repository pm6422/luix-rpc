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

public class RpcServiceException extends RpcAbstractException {

    private static final long serialVersionUID = -631851340032202804L;

    public RpcServiceException() {
        super(RpcErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(RpcErrorMsg RpcErrorMsg) {
        super(RpcErrorMsg);
    }

    public RpcServiceException(String message) {
        super(message, RpcErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(String message, RpcErrorMsg RpcErrorMsg) {
        super(message, RpcErrorMsg);
    }

    public RpcServiceException(String message, Throwable cause) {
        super(message, cause, RpcErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(String message, Throwable cause, RpcErrorMsg RpcErrorMsg) {
        super(message, cause, RpcErrorMsg);
    }

    public RpcServiceException(Throwable cause) {
        super(cause, RpcErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(Throwable cause, RpcErrorMsg RpcErrorMsg) {
        super(cause, RpcErrorMsg);
    }
}
