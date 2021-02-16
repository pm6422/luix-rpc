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

package org.infinity.rpc.core.server.response;

public interface Future {
    /**
     * cancel the task
     *
     * @return true: cancelled successfully, false: or else
     */
    boolean cancel();

    /**
     * task cancelled
     *
     * @return true: already cancelled, false: or else
     */
    boolean isCancelled();

    /**
     * task is complete : normal or exception
     *
     * @return true: done, false: or else
     */
    boolean isDone();

    /**
     * isDone() & normal
     *
     * @return true: success, false: or else
     */
    boolean isSuccess();

    /**
     * if task is success, return the result.
     *
     * @return RPC response result
     */
    Object getResult();

    /**
     * if task is done or cancel, return the exception
     *
     * @return exception
     */
    Exception getException();

    /**
     * add future listener , when task is success，failure, timeout, cancel, it will be called
     *
     * @param listener listener
     */
    void addListener(FutureListener listener);

}
