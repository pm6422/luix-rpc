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

public abstract class RpcErrorConstants {
    // service error status 503
    public static final int         SERVICE_DEFAULT_ERROR_CODE                     = 10001;
    public static final int         SERVICE_REJECT_ERROR_CODE                      = 10002;
    public static final int         SERVICE_TIMEOUT_ERROR_CODE                     = 10003;
    public static final int         SERVICE_TASK_CANCEL_ERROR_CODE                 = 10004;
    // service error status 404
    public static final int         SERVICE_NOT_FOUND_ERROR_CODE                   = 10101;
    // service error status 403
    public static final int         SERVICE_REQUEST_LENGTH_OUT_OF_LIMIT_ERROR_CODE = 10201;
    // framework error
    public static final int         FRAMEWORK_DEFAULT_ERROR_CODE                   = 20001;
    public static final int         FRAMEWORK_ENCODE_ERROR_CODE                    = 20002;
    public static final int         FRAMEWORK_DECODE_ERROR_CODE                    = 20003;
    public static final int         FRAMEWORK_INIT_ERROR_CODE                      = 20004;
    public static final int         FRAMEWORK_EXPORT_ERROR_CODE                    = 20005;
    public static final int         FRAMEWORK_SERVER_ERROR_CODE                    = 20006;
    public static final int         FRAMEWORK_REFER_ERROR_CODE                     = 20007;
    public static final int         FRAMEWORK_REGISTER_ERROR_CODE                  = 20008;
    // biz exception
    public static final int      BIZ_DEFAULT_ERROR_CODE              = 30001;
    /**
     * service error start
     **/
    public static final RpcError SERVICE_DEFAULT_ERROR               = new RpcError(503, SERVICE_DEFAULT_ERROR_CODE, "service error");
    public static final RpcError SERVICE_REJECT                      = new RpcError(503, SERVICE_REJECT_ERROR_CODE, "service reject");
    public static final RpcError SERVICE_NOT_FOUND                   = new RpcError(404, SERVICE_NOT_FOUND_ERROR_CODE, "service not found");
    public static final RpcError SERVICE_TIMEOUT                     = new RpcError(503, SERVICE_TIMEOUT_ERROR_CODE, "service request timeout");
    public static final RpcError SERVICE_TASK_CANCEL                 = new RpcError(503, SERVICE_TASK_CANCEL_ERROR_CODE, "service task cancel");
    public static final RpcError SERVICE_REQUEST_LENGTH_OUT_OF_LIMIT = new RpcError(403,
            SERVICE_REQUEST_LENGTH_OUT_OF_LIMIT_ERROR_CODE, "servier requset data length over of limit");
    /**
     * framework error start
     **/
    public static final RpcError FRAMEWORK_DEFAULT_ERROR             = new RpcError(503, FRAMEWORK_DEFAULT_ERROR_CODE,
            "framework default error");

    /**
     * service error end
     **/
    public static final RpcError ENCODE_ERROR           = new RpcError(503, FRAMEWORK_ENCODE_ERROR_CODE, "Encode error");
    public static final RpcError FRAMEWORK_DECODE_ERROR =
            new RpcError(503, FRAMEWORK_DECODE_ERROR_CODE, "framework decode error");
    public static final RpcError FRAMEWORK_INIT_ERROR   = new RpcError(500, FRAMEWORK_INIT_ERROR_CODE, "framework init error");
    public static final RpcError FRAMEWORK_EXPORT_ERROR =
            new RpcError(503, FRAMEWORK_EXPORT_ERROR_CODE, "framework export error");
    public static final RpcError FRAMEWORK_REFER_ERROR  = new RpcError(503, FRAMEWORK_REFER_ERROR_CODE, "framework refer error");
    /**
     * biz error start
     **/
    public static final RpcError BIZ_DEFAULT_EXCEPTION  = new RpcError(503, BIZ_DEFAULT_ERROR_CODE, "provider error");
    /** biz error end **/
}
