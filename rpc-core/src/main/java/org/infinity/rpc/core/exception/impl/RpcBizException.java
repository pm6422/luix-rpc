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

public class RpcBizException extends RpcAbstractException {
    private static final long serialVersionUID = -3491276058323309898L;

    public RpcBizException() {
        super(RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(RpcError rpcError) {
        super(rpcError);
    }

    public RpcBizException(String message) {
        super(message, RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(String message, RpcError rpcError) {
        super(message, rpcError);
    }

    public RpcBizException(String message, Throwable cause) {
        super(message, cause, RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(String message, Throwable cause, RpcError rpcError) {
        super(message, cause, rpcError);
    }

    public RpcBizException(Throwable cause) {
        super(cause, RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(Throwable cause, RpcError rpcError) {
        super(cause, rpcError);
    }
}
