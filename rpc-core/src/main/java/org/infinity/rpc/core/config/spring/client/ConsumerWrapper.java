package org.infinity.rpc.core.config.spring.client;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * PRC consumer configuration wrapper
 * And the class implements the {@link FactoryBean} interface means that
 * the class is used as a factory for an object to expose, not directly as a bean instance that will be exposed itself.
 */
@Slf4j
@Data
@Builder
public class ConsumerWrapper implements DisposableBean {
    /**
     * The consumer interface class fully-qualified name
     */
    private             String   interfaceName;
    /**
     * The interface class of the consumer
     */
    protected           Class<?> interfaceClass;
    /**
     * The consumer instance simple name, also known as bean name
     */
    private             String   instanceName;
    /**
     * The consumer proxy instance, refer the return type of {@link org.infinity.rpc.core.client.proxy.RpcConsumerProxy#getProxy(Class)}
     */
    private             Object   proxyInstance;

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
