package org.infinity.rpc.utilities.spi.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NameAs {

    String value() default "";
}
