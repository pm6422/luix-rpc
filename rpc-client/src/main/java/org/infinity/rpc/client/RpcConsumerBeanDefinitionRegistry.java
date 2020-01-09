package org.infinity.rpc.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcConsumerBeanDefinitionRegistry implements BeanDefinitionRegistryPostProcessor {

    private static final String                RESOURCE_PATTERN       = "**/*.class";
    private              RpcClientProperties   rpcClientProperties;
    private              RpcClientProxy        rpcClientProxy;
    //生成的Bean名称到代理的Service Class的映射
    private static final Map<String, Class<?>> HSF_UNDERLYING_MAPPING = new ConcurrentHashMap<String, Class<?>>();

    public RpcConsumerBeanDefinitionRegistry(RpcClientProperties rpcClientProperties, RpcClientProxy rpcClientProxy) {
        this.rpcClientProperties = rpcClientProperties;
        this.rpcClientProxy = rpcClientProxy;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String[] basePackages = rpcClientProperties.getClient().getBasePackages();
        Set<Class<?>> candidates = scanPackages(basePackages);
        if (candidates.isEmpty()) {
            log.info("扫描指定包[{}]时未发现复合条件的类", basePackages.toString());
            return;
        }
        registerBeanDefinitions(candidates, registry);
    }

    /**
     * @param basePackages
     * @return
     */
    private Set<Class<?>> scanPackages(String[] basePackages) {
        Set<Class<?>> candidates = new HashSet<Class<?>>();
        for (String pkg : basePackages) {
            try {
                List<Class<?>> candidateClasses = findCandidateClasses(pkg);
                if (!CollectionUtils.isEmpty(candidateClasses)) {
                    for (Class<?> candidateClass : candidateClasses) {
                        for (; candidateClass != Object.class; candidateClass = candidateClass.getSuperclass()) {
                            try {
                                Field[] fields = candidateClass.getDeclaredFields();
                                for (Field field : fields) {
                                    field.setAccessible(true);
                                    Consumer annotation = field.getAnnotation(Consumer.class);
                                    if (annotation != null) {
                                        // Get the field
                                        candidates.add(field.getType());
                                    }
                                }
                            } catch (Exception e) {
                                //doNothing
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("扫描指定HSF基础包[{}]时出现异常", pkg);
                continue;
            }
        }
        return candidates;
    }

    /**
     * @param basePackage
     * @return
     * @throws IOException
     */
    private List<Class<?>> findCandidateClasses(String basePackage) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("开始扫描指定包{}下的所有类" + basePackage);
        }
        List<Class<?>> candidates = new ArrayList<Class<?>>();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + replaceDotByDelimiter(basePackage) + '/' + RESOURCE_PATTERN;

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory(resourceLoader);
        Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(packageSearchPath);
        for (Resource resource : resources) {
            MetadataReader reader = readerFactory.getMetadataReader(resource);
            Class<?> candidateClass = transform(reader.getClassMetadata().getClassName());

            if (candidateClass != null) {
                candidates.add(candidateClass);
                log.debug("扫描到符合要求基础类:{}" + candidateClass.getName());
            }
        }
        return candidates;
    }

    /**
     * 用"/"替换包路径中"."
     *
     * @param path
     * @return
     */
    private String replaceDotByDelimiter(String path) {
        return StringUtils.replace(path, ".", "/");
    }

    /**
     * @param className
     * @return
     */
    private Class<?> transform(String className) {
        Class<?> clazz = null;
        try {
            clazz = ClassUtils.forName(className, this.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            log.info("未找到指定类{%s}", className);
        }
        return clazz;
    }

    /**
     * 注册 Bean,
     * Bean的名称格式:
     *
     * @param internalClasses
     * @param registry
     */
    private void registerBeanDefinitions(Set<Class<?>> internalClasses, BeanDefinitionRegistry registry) {
        for (Class<?> clazz : internalClasses) {
            if (HSF_UNDERLYING_MAPPING.values().contains(clazz)) {
                log.debug("重复扫描{}类,忽略重复注册", clazz.getName());
                continue;
            }

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();

            definition.getPropertyValues().add("interfaceClass", clazz);

            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) registry;
            RpcClientProperties rpcClientProperties = beanFactory.getBean(RpcClientProperties.class);

            RpcClientProxy rpcClientProxyBean = beanFactory.getBean(RpcClientProxy.class);
            Object proxy = rpcClientProxyBean.getProxy(clazz);
//            definition.getPropertyValues().add("value", proxy);
//            definition.setBeanClass(InterfaceFactoryBean.class);
            definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            log.debug("注册[{}]Bean", clazz.getName());
            registry.registerBeanDefinition(ClassUtils.getShortNameAsProperty(clazz), definition);
            HSF_UNDERLYING_MAPPING.put(ClassUtils.getShortNameAsProperty(clazz), clazz);

//            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(Consumer.class);
//            GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
//
//            definition.getPropertyValues().add("interfaceClass", Consumer.class);
//
//            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) registry;
//
//            RpcClientProxy rpcClientProxyBean = beanFactory.getBean(RpcClientProxy.class);
//            Object proxy = rpcClientProxyBean.getProxy(Consumer.class);
////            definition.getPropertyValues().add("value", proxy);
////            definition.setBeanClass(InterfaceFactoryBean.class);
//            definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
//            registry.registerBeanDefinition(ClassUtils.getShortNameAsProperty(Consumer.class), definition);
//
//
////        RootBeanDefinition consumerBean = new RootBeanDefinition(Consumer.class);
////        registry.registerBeanDefinition("hello", consumerBean);
        }
    }
}