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

package org.infinity.rpc.core.exchange.transport.constants;

public enum ChannelState {
    /**
     * Uninitialized state
     */
    UNINITIALIZED(0),
    /**
     * Initialized state
     */
    INITIALIZED(1),
    /**
     * Active state
     */
    ACTIVE(2),
    /**
     * Inactive state
     */
    INACTIVE(3),
    /**
     * Closed state
     */
    CLOSED(4);

    public final int value;

    ChannelState(int value) {
        this.value = value;
    }

    public boolean isUninitialized() {
        return this == UNINITIALIZED;
    }

    public boolean isInitialized() {
        return this == INITIALIZED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isInactive() {
        return this == INACTIVE;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
