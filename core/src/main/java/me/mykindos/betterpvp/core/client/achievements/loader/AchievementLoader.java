package me.mykindos.betterpvp.core.client.achievements.loader;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.ConfigLoadedAchievement;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.IConfigAchievementLoader;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;

@CustomLog
public abstract class AchievementLoader extends Loader {
    private final AchievementManager achievementManager;

    public AchievementLoader(BPvPPlugin plugin, AchievementManager achievementManager) {
        super(plugin);
        this.achievementManager = achievementManager;
        count = 0;
    }

    @Override
    public void load(Class<?> clazz) {
        IAchievement achievement = (IAchievement) plugin.getInjector().getInstance(clazz);
        System.out.println(1);
        plugin.getInjector().injectMembers(achievement);
        achievement.loadConfig(plugin.getConfig("achievements"));
        achievementManager.addObject(achievement.getNamespacedKey().asString(), achievement);
        count++;

    }

    public void loadLoader(Class<? extends IConfigAchievementLoader> clazz) {
        IConfigAchievementLoader<?> achievementLoader = plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(achievementLoader);
        Collection<? extends IAchievement> achievements = achievementLoader.loadAchievements(plugin.getConfig("achievements"));
        plugin.saveConfig();
        for (IAchievement achievement : achievements) {
            //these achievements are injected by the loader
            achievementManager.addObject(achievement.getNamespacedKey().asString(), achievement);
            count++;
        }
        plugin.saveConfig();
    }

    public void loadAllLoaderAchievements(Set<Class<? extends IConfigAchievementLoader>> classes) {
        count = 0;
        log.error("Start Load Achievements for {}", plugin.getName()).submit();
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            loadLoader(clazz);
            plugin.saveConfig();
        }
        log.error("Loaded {} Loader Achievements for {}", count, plugin.getName()).submit();
    }

    public void loadAllAchievements(Set<Class<? extends IAchievement>> classes) {
        count = 0;
        log.error("Start Load Achievements for {}", plugin.getName()).submit();
        for (var clazz : classes) {
            System.out.println(clazz.getName());
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class) || clazz.isAnnotationPresent(ConfigLoadedAchievement.class)) continue;
            load(clazz);
            plugin.saveConfig();
        }
        log.error("Loaded {} Achievements for {}", count, plugin.getName()).submit();
    }

    public abstract void loadAchievements(String packageName);
}
