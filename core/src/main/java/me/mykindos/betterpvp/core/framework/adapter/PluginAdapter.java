package me.mykindos.betterpvp.core.framework.adapter;

import javax.inject.Singleton;
import java.lang.annotation.*;

@Singleton
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PluginAdapters.class)
public @interface PluginAdapter {

    /**
     * The name of the plugin that this adapter is for.
     * @return The name of the plugin that this adapter is for.
     */
    String value();

}
