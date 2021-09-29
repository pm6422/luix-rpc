package org.infinity.luix.webcenter.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.infinity.luix.webcenter.utils.TraceIdUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Aspect
@Configuration
public class ExceptionTranslatorAdviceAspect {

    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, ExceptionHandler annotation) throws Throwable {
        // Proceed to execute method
        Object result = joinPoint.proceed();
        TraceIdUtils.remove();
        return result;
    }
}
