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
package org.infinity.rpc.core.config.spring.client;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.springframework.core.env.Environment;

/**
 * Consumer {@link org.infinity.rpc.core.client.annotation.Consumer @Consumer} Bean Builder
 */
public class ConsumerWrapperBeanNameGenerator {
    private static final String      SEPARATOR = ":";
    // Required
    private final        String      interfaceName;
    private final        Environment environment;
    // Optional
    private              String      version;
    private              String      group;

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ConsumerWrapperBeanNameGenerator(Class<?> interfaceClass, Environment environment) {
        this(interfaceClass.getName(), environment);
    }

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ConsumerWrapperBeanNameGenerator(String interfaceName, Environment environment) {
        this.interfaceName = interfaceName;
        this.environment = environment;
    }

    public static ConsumerWrapperBeanNameGenerator builder(Class<?> interfaceClass, Environment environment) {
        return new ConsumerWrapperBeanNameGenerator(interfaceClass, environment);
    }

    public static ConsumerWrapperBeanNameGenerator builder(String interfaceName, Environment environment) {
        return new ConsumerWrapperBeanNameGenerator(interfaceName, environment);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.isNotEmpty(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public ConsumerWrapperBeanNameGenerator group(String group) {
        this.group = group;
        return this;
    }

    public ConsumerWrapperBeanNameGenerator version(String version) {
        this.version = version;
        return this;
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder(RpcConsumerProxy.CONSUMER_PROXY_BEAN);
        // Required
        append(beanNameBuilder, interfaceName);
        // Optional
        append(beanNameBuilder, version);
        append(beanNameBuilder, group);
        // Build and remove last ":"
        String rawBeanName = beanNameBuilder.toString();
        // Resolve placeholders
        return environment.resolvePlaceholders(rawBeanName);
    }
}
