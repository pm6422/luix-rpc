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

package org.infinity.luix.core.exception.impl;

import org.infinity.luix.core.exception.RpcAbstractException;

/**
 * Business exception refers to an exception caused by a logical error in a service interface,
 * not all interfaces have errors
 */
public class RpcBizException extends RpcAbstractException {

    private static final long serialVersionUID = 1214118633333004121L;

    public RpcBizException() {
        super();
    }

    public RpcBizException(String message) {
        super(message);
    }

    public RpcBizException(Throwable cause) {
        super(cause);
    }

    public RpcBizException(String message, Throwable cause) {
        super(message, cause);
    }
}
