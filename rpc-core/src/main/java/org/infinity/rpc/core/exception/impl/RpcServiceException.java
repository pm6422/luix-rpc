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

package org.infinity.rpc.core.exception.impl;

import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorConstants;
import org.infinity.rpc.core.exception.RpcError;

public class RpcServiceException extends RpcAbstractException {

    private static final long serialVersionUID = -631851340032202804L;

    public RpcServiceException() {
        super(RpcErrorConstants.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(RpcError rpcError) {
        super(rpcError);
    }

    public RpcServiceException(String message) {
        super(message, RpcErrorConstants.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(String message, RpcError rpcError) {
        super(message, rpcError);
    }

    public RpcServiceException(String message, Throwable cause) {
        super(message, cause, RpcErrorConstants.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(String message, Throwable cause, RpcError rpcError) {
        super(message, cause, rpcError);
    }

    public RpcServiceException(Throwable cause) {
        super(cause, RpcErrorConstants.SERVICE_DEFAULT_ERROR);
    }

    public RpcServiceException(Throwable cause, RpcError rpcError) {
        super(cause, rpcError);
    }
}
