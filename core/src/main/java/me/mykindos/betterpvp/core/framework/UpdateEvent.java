package me.mykindos.betterpvp.core.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateEvent {

    /**
     *
     * @return Time in milliseconds to delay before running again
     */
    long delay() default 50;

    /**
     *
     * @return True if the timer should be run on its own thread
     */
    boolean isAsync() default false;

}
