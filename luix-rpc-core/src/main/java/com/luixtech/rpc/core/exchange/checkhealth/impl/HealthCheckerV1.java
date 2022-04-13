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

package com.luixtech.rpc.core.exchange.checkhealth.impl;

import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.client.request.impl.RpcCheckHealthRequest;
import com.luixtech.rpc.core.constant.ProviderConstants;
import com.luixtech.rpc.core.exchange.checkhealth.HealthChecker;
import com.luixtech.rpc.core.server.handler.InvocationHandleable;
import com.luixtech.rpc.core.server.handler.impl.DefaultCheckHealthHandlerWrapper;
import com.luixtech.uidgenerator.core.id.IdGenerator;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

/**
 *
 */
@SpiName(ProviderConstants.HEALTH_CHECKER_VAL_V1)
public class HealthCheckerV1 implements HealthChecker {
    @Override
    public InvocationHandleable wrap(InvocationHandleable handler) {
        return new DefaultCheckHealthHandlerWrapper(handler);
    }

    @Override
    public Requestable createRequest() {
        return new RpcCheckHealthRequest(IdGenerator.generateTimestampId());
    }
}
