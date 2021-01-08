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
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * RPC consumer wrapper bean name builder
 */
public class ConsumerWrapperBeanNameBuilder {
    public static final  String      CONSUMER_WRAPPER_BEAN_PREFIX = "ConsumerWrapperBean";
    private static final String      SEPARATOR                    = ":";
    /**
     * Consumer interface class
     */
    private final        Class<?>    interfaceClass;
    private final        Environment env;
    private              String      version;
    private              String      group;

    /**
     * Prevent instantiation of it outside the class
     */
    private ConsumerWrapperBeanNameBuilder(Class<?> interfaceClass, Environment env) {
        Assert.notNull(interfaceClass, "Consumer interface class must not be null!");
        Assert.notNull(env, "Environment must not be null!");
        this.interfaceClass = interfaceClass;
        this.env = env;
    }

    public static ConsumerWrapperBeanNameBuilder builder(Class<?> interfaceClass, Environment environment) {
        return new ConsumerWrapperBeanNameBuilder(interfaceClass, environment);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.isNotEmpty(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public ConsumerWrapperBeanNameBuilder group(String group) {
        this.group = group;
        return this;
    }

    public ConsumerWrapperBeanNameBuilder version(String version) {
        this.version = version;
        return this;
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder(CONSUMER_WRAPPER_BEAN_PREFIX);
        // Required
        append(beanNameBuilder, interfaceClass.getName());
        // Optional
        append(beanNameBuilder, version);
        append(beanNameBuilder, group);
        // Build and remove last ":"
        String rawBeanName = beanNameBuilder.toString();
        // Resolve placeholders
        return env.resolvePlaceholders(rawBeanName);
    }
}
