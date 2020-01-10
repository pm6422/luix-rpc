package org.infinity.rpc.client.registrar;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.RpcClientProperties;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcConsumerRegistrar implements BeanFactoryAware, InitializingBean, ImportBeanDefinitionRegistrar, EnvironmentAware {
    private static final String                RESOURCE_PATTERN         = "**/*.class";
    // 已经注册过的，用于去重复
    private static final Map<String, Class<?>> REGISTERED_BEAN_MAPPING  = new ConcurrentHashMap<String, Class<?>>();
    private              BeanFactory           beanFactory;
    private static final String                FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";
    private              RpcClientProperties   rpcClientProperties;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void setEnvironment(Environment environment) {
        Binder binder = Binder.get(environment);
        rpcClientProperties = binder.bind("spring.infinity-rpc", Bindable.of(RpcClientProperties.class)).get();
        Assert.notNull(rpcClientProperties, "Rpc client properties bean must be created!");
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
//        AnnotationAttributes annAttr = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableRpcClient.class.getName()));
        String[] basePackages = rpcClientProperties.getClient().getBasePackages();

        if (ObjectUtils.isEmpty(basePackages)) {
            basePackages = new String[]{ClassUtils.getPackageName(importingClassMetadata.getClassName())};
        }

        List<TypeFilter> includeFilters = new ArrayList<>();
        //增加一个包含的过滤器,扫描到的类只要不是抽象的,接口,枚举,注解,及匿名类那么就算是符合的
        includeFilters.add(new RpcConsumerTypeFilter());
//        List<TypeFilter> excludeFilters = extractTypeFilters(annAttr.getAnnotationArray("excludeFilters"));
        Set<Class<?>> candidates = scanPackages(basePackages, includeFilters);

        if (candidates.isEmpty()) {
            log.info("No @Consumer bean found", basePackages.toString());
            return;
        }
        //注册处理器后,为 对象注入环境配置信息
        //通过该类对对象进行进一步操作
        //registerHsfBeanPostProcessor(registry);
        //注册
        registerBeanDef(candidates, registry);
    }

    /**
     * @param classes
     * @return
     */
    private String[] getPackagesFromClasses(Class[] classes) {
        if (ObjectUtils.isEmpty(classes)) {
            return null;
        }
        List<String> basePackages = new ArrayList<String>(classes.length);
        for (Class<?> clazz : classes) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }
        return (String[]) basePackages.toArray();
    }

    /**
     * @param annAttrs
     * @return
     */
    private List<TypeFilter> extractTypeFilters(AnnotationAttributes[] annAttrs) {
        List<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
        for (AnnotationAttributes filter : annAttrs) {
            typeFilters.addAll(typeFiltersFor(filter));
        }
        return typeFilters;
    }

    /**
     * @param filterAttributes
     * @return
     */
    private List<TypeFilter> typeFiltersFor(AnnotationAttributes filterAttributes) {
        List<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
        FilterType filterType = filterAttributes.getEnum("type");

        for (Class<?> filterClass : filterAttributes.getClassArray("classes")) {
            switch (filterType) {
                case ANNOTATION:
                    Assert.isAssignable(Annotation.class, filterClass, "@ComponentScan 注解类型的Filter必须指定一个注解");
                    Class<Annotation> annotationType = (Class<Annotation>) filterClass;
                    typeFilters.add(new AnnotationTypeFilter(annotationType));
                    break;
                case ASSIGNABLE_TYPE:
                    typeFilters.add(new AssignableTypeFilter(filterClass));
                    break;
                case CUSTOM:
                    Assert.isAssignable(TypeFilter.class, filterClass, "@ComponentScan 自定义Filter必须实现TypeFilter接口");
                    TypeFilter filter = BeanUtils.instantiateClass(filterClass, TypeFilter.class);
                    typeFilters.add(filter);
                    break;
                default:
                    throw new IllegalArgumentException("当前TypeFilter不支持: " + filterType);
            }
        }
        return typeFilters;
    }

    /**
     * @param basePackages
     * @param includeFilters
     * @return
     */
    private Set<Class<?>> scanPackages(String[] basePackages, List<TypeFilter> includeFilters) {
        Set<Class<?>> candidates = new HashSet<Class<?>>();
        for (String pkg : basePackages) {
            try {
                List<Class<?>> candidateClasses = findCandidateClasses(pkg, includeFilters);
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
                                // doNothing
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Failed to scan @Consumer bean", pkg);
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
    private List<Class<?>> findCandidateClasses(String basePackage, List<TypeFilter> includeFilters) throws IOException {
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
            if (isCandidateResource(reader, readerFactory, includeFilters)) {
                Class<?> candidateClass = transform(reader.getClassMetadata().getClassName());

                if (candidateClass != null) {
                    candidates.add(candidateClass);
                    log.debug("扫描到符合要求基础类:{}" + candidateClass.getName());
                }
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
     * @param reader
     * @param readerFactory
     * @param includeFilters
     * @return
     * @throws IOException
     */
    private boolean isCandidateResource(MetadataReader reader, MetadataReaderFactory readerFactory, List<TypeFilter> includeFilters) throws IOException {
//        for (TypeFilter tf : excludeFilters) {
//            if (tf.match(reader, readerFactory)) {
//                return false;
//            }
//        }
        for (TypeFilter tf : includeFilters) {
            if (tf.match(reader, readerFactory)) {
                return true;
            }
        }
        return false;
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
    private void registerBeanDef(Set<Class<?>> internalClasses, BeanDefinitionRegistry registry) {
        for (Class<?> clazz : internalClasses) {
            if (REGISTERED_BEAN_MAPPING.values().contains(clazz)) {
                log.debug("Ignore the bean for already registered", clazz.getName());
                continue;
            }

            try {
                BeanDefinitionBuilder definitionBuilder = this.build(clazz);
                BeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();

                // Save in attributes
                beanDefinition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, clazz.getName());
                String beanName = ClassUtils.getShortNameAsProperty(clazz);
                registry.registerBeanDefinition(beanName, beanDefinition);
                log.debug("Registered RPC consumer service bean [{}]", clazz.getName());
                REGISTERED_BEAN_MAPPING.put(beanName, clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private BeanDefinitionBuilder build(Class consumerInterface) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RpcConsumerFactoryBean.class);
//        builder.getRawBeanDefinition().setSource();
        // Set 1st constructor arg RpcConsumerFactoryBean.class
        builder.addConstructorArgValue(consumerInterface);
        return builder;
    }
}
