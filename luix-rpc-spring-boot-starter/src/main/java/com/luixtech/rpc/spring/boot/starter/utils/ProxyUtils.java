package com.luixtech.rpc.spring.boot.starter.utils;


import org.springframework.aop.support.AopUtils;

public abstract class ProxyUtils {

    public static Class<?> getTargetClass(Object bean) {
        if (isProxyBean(bean)) {
            // Get class of the bean if it is a proxy bean
            return AopUtils.getTargetClass(bean);
        }
        return bean.getClass();
    }

    public static boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy(bean);
    }
}
