package org.infinity.rpc.client;

import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ConsumerAnnotationBean implements BeanPostProcessor, BeanFactoryPostProcessor {
    public static final Pattern                             COMMA_SPLIT_PATTERN       = Pattern.compile("\\s*[,]+\\s*");
    private             ApplicationContext                  applicationContext;
    private             String[]                            consumerScanPackages;
    private final       Map<String, RpcConsumerFactoryBean> rpcConsumerFactoryBeanMap = new ConcurrentHashMap<String, RpcConsumerFactoryBean>();


    public ConsumerAnnotationBean(ApplicationContext applicationContext, String consumerScanPackages) {
        Assert.notNull(applicationContext, "Application context must not be null!");
        this.applicationContext = applicationContext;
        this.consumerScanPackages = (StringUtils.isEmpty(consumerScanPackages)) ? null : COMMA_SPLIT_PATTERN.split(consumerScanPackages);
    }

    /**
     * Inject RPC consumer proxy
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = getTargetClass(bean);

        if (!matchScanPackages(clazz)) {
            return bean;
        }

        RpcConsumerProxy rpcConsumerProxy = applicationContext.getBean(RpcConsumerProxy.class);
        setConsumerOnMethod(bean, clazz, rpcConsumerProxy);
        setConsumerOnField(bean, clazz, rpcConsumerProxy);
        return bean;
    }

    private Class getTargetClass(Object bean) {
        if (isProxyBean(bean)) {
            return AopUtils.getTargetClass(bean);
        }
        return bean.getClass();
    }

    private boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy(bean);
    }

    private boolean matchScanPackages(Class clazz) {
        if (consumerScanPackages == null || consumerScanPackages.length == 0) {
            return true;
        }
        String beanClassName = clazz.getName();
        return Arrays.asList(consumerScanPackages).stream().anyMatch(pkg -> beanClassName.startsWith(pkg));
    }

    private void setConsumerOnMethod(Object bean, Class clazz, RpcConsumerProxy rpcConsumerProxy) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.length() > 3 && methodName.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    Consumer consumerMethod = method.getAnnotation(Consumer.class);
                    if (consumerMethod != null) {
                        // if setter method annotated with the @Consumer
                        Object value = getConsumerProxy(consumerMethod, method.getParameterTypes()[0], rpcConsumerProxy);
                        if (value != null) {
                            method.invoke(bean, new Object[]{value});
                        }
                    }
                } catch (Throwable t) {
                    throw new BeanInitializationException("Failed to set RPC consumer proxy by setter method " + methodName
                            + " in class " + bean.getClass().getName(), t);
                }
            }
        }
    }

    private void setConsumerOnField(Object bean, Class clazz, RpcConsumerProxy rpcConsumerProxy) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Consumer consumerField = field.getAnnotation(Consumer.class);
                if (consumerField != null) {
                    Object value = getConsumerProxy(consumerField, field.getType(), rpcConsumerProxy);
                    if (value != null) {
                        // if field annotated with the @Consumer
                        field.set(bean, value);
                    }
                }
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to set RPC consumer proxy by field reflection" + field.getName()
                        + " in class " + bean.getClass().getName(), t);
            }
        }
    }

    private <T> Object getConsumerProxy(Consumer consumer, Class<T> consumerClass, RpcConsumerProxy rpcConsumerProxy) throws Exception {
        String interfaceName;
        if (!void.class.equals(consumer.interfaceClass())) {
            interfaceName = consumer.interfaceClass().getName();
        } else if (consumerClass.isInterface()) {
            interfaceName = consumerClass.getName();
        } else {
            throw new IllegalStateException("The consumer must be declared as an interface or specify the interfaceClass attribute value of @Consumer annotation!");
        }

        String key = interfaceName;
        RpcConsumerFactoryBean rpcConsumerFactoryBean = rpcConsumerFactoryBeanMap.get(key);
        if (rpcConsumerFactoryBean == null) {
            Class<T> consumerInterface;
            if (void.class.equals(consumer.interfaceClass()) && consumerClass.isInterface()) {
                consumerInterface = consumerClass;
            } else {
                consumerInterface = (Class<T>) consumer.interfaceClass();
            }

            rpcConsumerFactoryBean = new RpcConsumerFactoryBean<T>(consumerInterface, rpcConsumerProxy);
            rpcConsumerFactoryBeanMap.putIfAbsent(key, rpcConsumerFactoryBean);
        }
        return rpcConsumerFactoryBean.getObject();
    }

    /**
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//        if (consumerScanPackages == null || consumerScanPackages.length == 0) {
//            return;
//        }
//        if (beanFactory instanceof BeanDefinitionRegistry) {
//            try {
//                // init scanner
//                Class<?> scannerClass = ClassUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
//                        ConsumerAnnotationBean.class.getClassLoader());
//                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class})
//                        .newInstance(new Object[]{(BeanDefinitionRegistry) beanFactory, true});
//                // add filter
//                Class<?> filterClass = ClassUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter",
//                        ConsumerAnnotationBean.class.getClassLoader());
//                Object filter = filterClass.getConstructor(Class.class).newInstance(MotanService.class);
//                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter",
//                        ClassUtils.forName("org.springframework.core.type.filter.TypeFilter", ConsumerAnnotationBean.class.getClassLoader()));
//                addIncludeFilter.invoke(scanner, filter);
//                // scan packages
//                Method scan = scannerClass.getMethod("scan", new Class<?>[]{String[].class});
//                scan.invoke(scanner, new Object[]{consumerScanPackages});
//            } catch (Throwable e) {
//                // spring 2.0
//            }
//        }
    }
}
