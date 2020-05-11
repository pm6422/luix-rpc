package org.infinity.rpc.utilities.annotation;

import java.lang.annotation.*;

/**
 * A symbol used to identify the event mechanism connect to the method
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.CLASS)
public @interface Event {
}
