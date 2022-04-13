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

package com.luixtech.rpc.core.server.response;

public interface FutureListener {

    /**
     * <pre>
     * 		建议做一些比较简单的低功耗的操作
     *
     * 		注意一些反模式：
     *
     * 		1) 死循环：
     * 			operationComplete(Future future) {
     * 					......
     * 				future.addListener(this);  // 类似于这种操作，后果你懂的
     * 					......
     *            }
     *
     * 		2）耗资源操作或者慢操作：
     * 			operationComplete(Future future) {
     * 					......
     * 				Thread.sleep(500);
     * 					......
     *            }
     *
     * </pre>
     *
     * @param future future
     * @throws Exception if any exception throws
     */
    void operationComplete(Future future) throws Exception;

}
