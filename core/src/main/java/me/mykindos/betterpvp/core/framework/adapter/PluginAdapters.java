package me.mykindos.betterpvp.core.framework.adapter;

import java.lang.annotation.*;

/**
 * Represents a collection of PluginAdapter annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface PluginAdapters {

    PluginAdapter[] value();

}
