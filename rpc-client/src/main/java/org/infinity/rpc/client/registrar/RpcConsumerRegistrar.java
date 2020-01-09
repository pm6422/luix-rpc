package org.infinity.rpc.client.registrar;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.RpcClientProperties;
import org.infinity.rpc.client.RpcClientProxy;
import org.infinity.rpc.client.annotation.Consumer;
import org.infinity.rpc.registry.ZookeeperRpcServerDiscovery;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
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
public class RpcConsumerRegistrar implements ImportBeanDefinitionRegistrar {
    private static final String                RESOURCE_PATTERN       = "**/*.class";
    //生成的Bean名称到代理的Service Class的映射
    private static final Map<String, Class<?>> HSF_UNDERLYING_MAPPING = new ConcurrentHashMap<String, Class<?>>();
    public               String                RPC_CLIENT_PROXY       = "rpcClientProxy";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes annAttr = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableRpcClient.class.getName()));
        String[] basePackages = annAttr.getStringArray("value");

        if (ObjectUtils.isEmpty(basePackages)) {
            basePackages = annAttr.getStringArray("basePackages");
        }
        if (ObjectUtils.isEmpty(basePackages)) {
            basePackages = getPackagesFromClasses(annAttr.getClassArray("basePackageClasses"));
        }
        if (ObjectUtils.isEmpty(basePackages)) {
            basePackages = new String[]{ClassUtils.getPackageName(importingClassMetadata.getClassName())};
        }

        List<TypeFilter> includeFilters = extractTypeFilters(annAttr.getAnnotationArray("includeFilters"));
        //增加一个包含的过滤器,扫描到的类只要不是抽象的,接口,枚举,注解,及匿名类那么就算是符合的
        includeFilters.add(new HsfTypeFilter());
        List<TypeFilter> excludeFilters = extractTypeFilters(annAttr.getAnnotationArray("excludeFilters"));
        Set<Class<?>> candidates = scanPackages(basePackages, includeFilters, excludeFilters);

        if (candidates.isEmpty()) {
            log.info("扫描指定包[{}]时未发现复合条件的类", basePackages.toString());
            return;
        }
        //注册处理器后,为 对象注入环境配置信息
        //通过该类对对象进行进一步操作
        //registerHsfBeanPostProcessor(registry);
        //注册
        registerBeanDefinitions(candidates, registry);
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
                    Assert.isAssignable(Annotation.class, filterClass, "@HsfComponentScan 注解类型的Filter必须指定一个注解");
                    Class<Annotation> annotationType = (Class<Annotation>) filterClass;
                    typeFilters.add(new AnnotationTypeFilter(annotationType));
                    break;
                case ASSIGNABLE_TYPE:
                    typeFilters.add(new AssignableTypeFilter(filterClass));
                    break;
                case CUSTOM:
                    Assert.isAssignable(TypeFilter.class, filterClass, "@HsfComponentScan 自定义Filter必须实现TypeFilter接口");
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
     * @param excludeFilters
     * @return
     */
    private Set<Class<?>> scanPackages(String[] basePackages, List<TypeFilter> includeFilters, List<TypeFilter> excludeFilters) {
        Set<Class<?>> candidates = new HashSet<Class<?>>();
        for (String pkg : basePackages) {
            try {
                List<Class<?>> candidateClasses = findCandidateClasses(pkg, includeFilters, excludeFilters);
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
    private List<Class<?>> findCandidateClasses(String basePackage, List<TypeFilter> includeFilters, List<TypeFilter> excludeFilters) throws IOException {
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
            if (isCandidateResource(reader, readerFactory, includeFilters, excludeFilters)) {
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
     * @param excludeFilters
     * @return
     * @throws IOException
     */
    private boolean isCandidateResource(MetadataReader reader, MetadataReaderFactory readerFactory, List<TypeFilter> includeFilters,
                                        List<TypeFilter> excludeFilters) throws IOException {
        for (TypeFilter tf : excludeFilters) {
            if (tf.match(reader, readerFactory)) {
                return false;
            }
        }
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

            try {
                ZookeeperRpcServerDiscovery rpcServerDiscovery = new ZookeeperRpcServerDiscovery("127.0.0.1:2181");
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcServerDiscovery);
                Object proxy = rpcClientProxy.getProxy(clazz);

//                RpcClientProxy rpcClientProxyBean = beanFactory.getBean(RpcClientProxy.class);
//                Object proxy = rpcClientProxyBean.getProxy(clazz);

                definition.getPropertyValues().add("interfaceClass", clazz);
                definition.getPropertyValues().add("value", proxy);
                definition.setBeanClass(proxy.getClass());
                definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
                log.debug("注册[{}]Bean", clazz.getName());
                String beanName = ClassUtils.getShortNameAsProperty(clazz);
                beanFactory.registerBeanDefinition(beanName, definition);
                HSF_UNDERLYING_MAPPING.put(ClassUtils.getShortNameAsProperty(clazz), clazz);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
