package org.infinity.rpc.core.config.spring.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * PRC consumer configuration wrapper
 */
@Slf4j
@Data
public class ConsumerWrapper implements FactoryBean, DisposableBean {
    /**
     * The consumer interface fully-qualified name
     */
    private   String           interfaceName;
    /**
     * The interface class of the consumer
     */
    protected Class<?>         interfaceClass;
    /**
     * The consumer instance simple name, also known as bean name
     */
    private   String           instanceName;
    /**
     * The consumer proxy instance
     */
    private   RpcConsumerProxy instance;

    /**
     * Get the consumer proxy
     *
     * @return consumer proxy
     * @throws Exception if any exception occurred
     */
    @Override
    public Object getObject() throws Exception {
        return instance;
    }

    /**
     * Get the consumer interface class
     *
     * @return consumer interface class
     */
    @Override
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    /**
     * Check whether is a singleton or not
     *
     * @return true: singleton, false: not singleton
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        // Leave blank intentionally for now
    }

    private Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }

        return interfaceClass;
    }
}
