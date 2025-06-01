package me.mykindos.betterpvp.core.client.achievements.types.loaded;


import com.google.inject.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.meta.TypeQualifier;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;


/**
 * Indicates this {@link IAchievement} is loaded by a {@link IConfigAchievementLoader} and not by reflection
 */
@Target(ElementType.TYPE)
@TypeQualifier(applicableTo = IAchievement.class)
@Singleton
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ConfigLoadedAchievement {
}
