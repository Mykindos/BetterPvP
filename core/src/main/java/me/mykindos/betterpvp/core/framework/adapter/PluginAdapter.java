package me.mykindos.betterpvp.core.framework.adapter;

import com.google.inject.Singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Singleton
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(PluginAdapters.class)
public @interface PluginAdapter {

    /**
     * The name of the plugin that this adapter is for.
     * @return The name of the plugin that this adapter is for.
     */
    String value();

    /**
     * The name of the method that should be called when the plugin is loaded. This method
     * should be public and have no parameters.
     * @return The name of the method that should be called when the plugin is loaded.
     */
    String loadMethodName() default "load";

}
