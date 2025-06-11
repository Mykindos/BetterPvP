package me.mykindos.betterpvp.core.client.stats.formatter.category;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubStatCategory {
    Class<? extends IStatCategory> value();
}
