package org.infinity.rpc.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
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
@Configuration
public class RpcConsumerConfiguration implements ApplicationContextAware {

    private static final String                RESOURCE_PATTERN       = "**/*.class";
    //生成的Bean名称到代理的Service Class的映射
    private static final Map<String, Class<?>> HSF_UNDERLYING_MAPPING = new ConcurrentHashMap<String, Class<?>>();

    @Autowired
    private RpcClientProperties rpcClientProperties;

    @Autowired
    private RpcClientProxy rpcClientProxy;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        registerBeans(applicationContext);
    }

    private void registerBeans(ApplicationContext applicationContext) {
        String[] basePackages = rpcClientProperties.getClient().getBasePackages();
        Set<Class<?>> candidates = scanPackages(basePackages);
        if (candidates.isEmpty()) {
            log.info("扫描指定包[{}]时未发现复合条件的类", basePackages.toString());
            return;
        }
        registerBeanDefinitions(candidates, applicationContext);
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
     * @param applicationContext
     */
    private void registerBeanDefinitions(Set<Class<?>> internalClasses, ApplicationContext applicationContext) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        for (Class<?> clazz : internalClasses) {
            if (HSF_UNDERLYING_MAPPING.values().contains(clazz)) {
                log.debug("重复扫描{}类,忽略重复注册", clazz.getName());
                continue;
            }

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();

            definition.getPropertyValues().add("interfaceClass", clazz);
            Object proxy = rpcClientProxy.getProxy(clazz);
            definition.getPropertyValues().add("value", proxy);
            definition.setBeanClass(proxy.getClass());
            definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            log.debug("注册[{}]Bean", clazz.getName());
            String beanName = ClassUtils.getShortNameAsProperty(clazz);
            beanFactory.registerBeanDefinition(beanName, definition);
            HSF_UNDERLYING_MAPPING.put(ClassUtils.getShortNameAsProperty(clazz), clazz);
        }
    }
}
