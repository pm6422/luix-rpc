/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infinity.rpc.core.config.spring.server.stub;

import org.infinity.rpc.core.config.spring.bean.ProviderConsumerStubBeanNameBuilder;
import org.springframework.core.env.Environment;

/**
 * RPC provider stub bean name builder
 */
public class ProviderStubBeanNameBuilder extends ProviderConsumerStubBeanNameBuilder {

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderStubBeanNameBuilder(Class<?> interfaceClass, Environment env) {
        super(interfaceClass, env);
    }

    public static ProviderStubBeanNameBuilder builder(Class<?> interfaceClass, Environment environment) {
        return new ProviderStubBeanNameBuilder(interfaceClass, environment);
    }

    public ProviderStubBeanNameBuilder group(String group) {
        this.group = group;
        return this;
    }

    public ProviderStubBeanNameBuilder version(String version) {
        this.version = version;
        return this;
    }

    public String build() {
        return super.build(PROVIDER_STUB_BEAN_PREFIX);
    }
}
