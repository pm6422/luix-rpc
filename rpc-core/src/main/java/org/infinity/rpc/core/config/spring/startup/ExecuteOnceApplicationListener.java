package org.infinity.rpc.core.config.spring.startup;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import java.util.Objects;

/**
 * The spring application listener which is always triggered once.
 */
abstract class ExecuteOnceApplicationListener implements ApplicationContextAware, ApplicationListener {

    private ApplicationContext applicationContext;

    @Override
    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public final void onApplicationEvent(ApplicationEvent event) {
        if (isOriginalEventSource(event) && event instanceof ApplicationContextEvent) {
            onApplicationContextEvent((ApplicationContextEvent) event);
        }
    }

    /**
     * The subclass overrides this method to handle {@link ApplicationContextEvent}
     *
     * @param event {@link ApplicationContextEvent}
     */
    protected abstract void onApplicationContextEvent(ApplicationContextEvent event);

    /**
     * Check whether is original {@link ApplicationContext} as the event source
     *
     * @param event {@link ApplicationEvent}
     * @return
     */
    private boolean isOriginalEventSource(ApplicationEvent event) {
        // Current ApplicationListener is not a Spring Bean, just was added into Spring's ConfigurableApplicationContext
        return (applicationContext == null) || Objects.equals(applicationContext, event.getSource());
    }
}
