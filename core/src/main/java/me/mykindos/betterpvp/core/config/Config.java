package me.mykindos.betterpvp.core.config;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface Config {

    String path();

    String defaultValue() default "";

    String configName() default "config";

}
