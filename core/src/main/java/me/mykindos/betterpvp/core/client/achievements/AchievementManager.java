package me.mykindos.betterpvp.core.client.achievements;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Modifier;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.reflections.Reflections;

@Singleton
@CustomLog
public class AchievementManager extends Manager<IAchievement> {

    private final Core core;
    @Inject
    public AchievementManager(Core core) {
        this.core = core;
    }

    //todo refactor to a loader
    public void loadAchievements(){
        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends IAchievement>> classes = reflections.getSubTypesOf(IAchievement.class);
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            IAchievement achievement = core.getInjector().getInstance(clazz);
            core.getInjector().injectMembers(achievement);

            addObject(achievement.getName(), achievement);

        }

        log.error("Loaded " + objects.size() + " achievements").submit();
        core.saveConfig();
    }
}
