package org.infinity.rpc.core.utils.name;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.client.annotation.RpcConsumer;

import java.util.Iterator;
import java.util.Map;

/**
 * RPC consumer {@link RpcConsumer} stub bean name builder
 * Consumer stub bean name is different from provider stub name,
 * RPC consumer can have various implementations depending on different annotation attributes of @Consumer
 */
public class ConsumerStubBeanNameBuilder {

    public static final    String              PREFIX    = "ConsumerStub";
    protected static final String              SEPARATOR = ":";
    /**
     * Consumer interface class(Required)
     */
    protected final        String              interfaceClassName;
    /**
     *
     */
    protected              Map<String, Object> attributes;
    /**
     * Use prefix indicator
     */
    protected              boolean             usePrefix = true;


    /**
     * Prevent instantiation of it outside the class
     */
    private ConsumerStubBeanNameBuilder(String interfaceClassName) {
        Validate.notEmpty(interfaceClassName, "Interface class name must NOT be empty!");
        this.interfaceClassName = interfaceClassName;
    }

    public static ConsumerStubBeanNameBuilder builder(String interfaceClassName) {
        return new ConsumerStubBeanNameBuilder(interfaceClassName);
    }

    public ConsumerStubBeanNameBuilder attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public ConsumerStubBeanNameBuilder disablePrefix() {
        this.usePrefix = false;
        return this;
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder(usePrefix ? PREFIX : StringUtils.EMPTY);
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
