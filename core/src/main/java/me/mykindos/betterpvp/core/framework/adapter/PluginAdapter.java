package me.mykindos.betterpvp.core.framework.adapter;

import javax.inject.Singleton;
import java.lang.annotation.*;

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
