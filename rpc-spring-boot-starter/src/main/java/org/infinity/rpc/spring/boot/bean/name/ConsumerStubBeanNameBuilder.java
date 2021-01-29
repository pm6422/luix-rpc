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
package org.infinity.rpc.spring.boot.bean.name;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.Map;

/**
 * RPC consumer {@link org.infinity.rpc.core.client.annotation.Consumer} stub bean name builder
 * Consumer stub bean name is different from provider stub name,
 * RPC consumer can have various implementations depending on different annotation attributes of @Consumer
 */
public class ConsumerStubBeanNameBuilder {

    public static final    String               CONSUMER_STUB_BEAN_PREFIX = "ConsumerStub";
    protected static final String               SEPARATOR                 = ":";
    /**
     * Provider interface class(Required)
     */
    protected final        String               interfaceClassName;
    /**
     * Environment(Required)
     */
    protected final        Environment          env;
    /**
     *
     */
    protected              AnnotationAttributes attributes;


    /**
     * Prevent instantiation of it outside the class
     */
    private ConsumerStubBeanNameBuilder(String interfaceClassName, Environment env) {
        Assert.hasText(interfaceClassName, "Interface class name must NOT be empty!");
        Assert.notNull(env, "Environment must NOT be null!");
        this.interfaceClassName = interfaceClassName;
        this.env = env;
    }

    public static ConsumerStubBeanNameBuilder builder(String interfaceClassName, Environment environment) {
        return new ConsumerStubBeanNameBuilder(interfaceClassName, environment);
    }

    public ConsumerStubBeanNameBuilder attributes(AnnotationAttributes attributes) {
        this.attributes = attributes;
        return this;
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder(CONSUMER_STUB_BEAN_PREFIX);
        // Required
        append(beanNameBuilder, interfaceClassName);
        // Optional
        if (MapUtils.isNotEmpty(attributes)) {
            beanNameBuilder.append('(');
            Iterator<Map.Entry<String, Object>> iterator = attributes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                beanNameBuilder.append(entry.getKey()).append('=').append(entry.getValue());
                if (iterator.hasNext()) {
                    beanNameBuilder.append(',');
                }
            }
            beanNameBuilder.append(')');
        }
        return beanNameBuilder.toString();
    }

    protected static void append(StringBuilder builder, String value) {
        if (StringUtils.isNotEmpty(value)) {
            if (builder.length() > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(value);
        }
    }
}
