package me.mykindos.betterpvp.core.utilities.model;


import com.google.inject.Singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * An annotation that means this class should not be retrieved by reflection
 */
@Target(ElementType.TYPE)
@Singleton
@Retention(value = RetentionPolicy.RUNTIME)
public @interface NoReflection {
}
