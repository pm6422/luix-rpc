package org.infinity.rpc.client.registrar;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;

/**
 * 自定义RPC consumer类型过滤，过滤抽象类,接口,注解,枚举,内部类及匿名类
 */
@Slf4j
public class RpcConsumerTypeFilter extends AbstractClassTestingTypeFilter {

    @Override
    protected boolean match(ClassMetadata metadata) {
        Class<?> clazz = transformToClass(metadata.getClassName());
        if (clazz == null) {
            return false;
        }

        boolean found = false;
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Consumer annotation = field.getAnnotation(Consumer.class);
                    if (annotation != null) {
                        found = true;
                        break;
                    }
                }

            } catch (Exception e) {
                //doNothing
            }
        }

        if (!found) {
            return false;
        }

        if (isAnnotatedBySpring(clazz)) {
            throw new IllegalStateException("Class ".concat(clazz.getName()).concat(" can NOT be marked as Spring related and @Consumer annotation"));
        }
        //过滤抽象类,接口,注解,枚举,内部类及匿名类
        return !metadata.isAbstract() && !clazz.isInterface() && !clazz.isAnnotation() && !clazz.isEnum()
                && !clazz.isMemberClass() && !clazz.getName().contains("$");
    }

    /**
     * @param className
     * @return
     */
    private Class<?> transformToClass(String className) {
        Class<?> clazz = null;
        try {
            clazz = ClassUtils.forName(className, this.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            log.info("No found RPC consumer");
        }
        return clazz;
    }

    /**
     * @param clazz
     * @return
     */
    private boolean isAnnotatedBySpring(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class) || clazz.isAnnotationPresent(Configuration.class)
                || clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Repository.class)
                || clazz.isAnnotationPresent(Controller.class);
    }
}