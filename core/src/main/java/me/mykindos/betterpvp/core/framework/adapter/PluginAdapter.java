package me.mykindos.betterpvp.core.framework.adapter;

import javax.inject.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Singleton
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginAdapter {

    /**
     * The name of the plugin that this adapter is for.
     * @return The name of the plugin that this adapter is for.
     */
    String value();

}
