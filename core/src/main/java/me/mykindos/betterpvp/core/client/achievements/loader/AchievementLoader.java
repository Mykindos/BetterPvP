package me.mykindos.betterpvp.core.client.achievements.loader;

import java.lang.reflect.Modifier;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementManager;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;

@CustomLog
public abstract class AchievementLoader extends Loader {
    private final AchievementManager achievementManager;

    public AchievementLoader(BPvPPlugin plugin, AchievementManager achievementManager) {
        super(plugin);
        this.achievementManager = achievementManager;
    }

    @Override
    public void load(Class<?> clazz) {
        IAchievement achievement = (IAchievement) plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(achievement);
        //todo load configs
        achievementManager.addObject(achievement.getNamespacedKey().asString(), achievement);
        count++;
    }

    public void loadAll(Set<Class<? extends IAchievement>> clazzes) {
        for (var clazz : clazzes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) return;
            if(clazz.isAnnotationPresent(Deprecated.class)) return;
            load(clazz);

            plugin.saveConfig();

            log.info("Loaded {} Achievements for {}", count, plugin.getName());
        }
    }

    public abstract void loadAchievements(String packageName);
}
