package me.mykindos.betterpvp.core.client.achievements.loader;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.IConfigAchievementLoader;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import me.mykindos.betterpvp.core.listener.loader.ListenerLoader;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import org.bukkit.event.Listener;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

@CustomLog
public abstract class AchievementLoader extends Loader {
    private final AchievementManager achievementManager;

    protected AchievementLoader(BPvPPlugin plugin, AchievementManager achievementManager) {
        super(plugin);
        this.achievementManager = achievementManager;
        count = 0;
    }

    @Override
    public void load(Class<?> clazz) {
        IAchievement achievement = (IAchievement) plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(achievement);
        achievement.loadConfig(plugin.getConfig("achievements"));
        achievementManager.addObject(achievement.getNamespacedKey().asString(), achievement);
        if (achievement instanceof Listener listener) {
            ListenerLoader.register(plugin, listener);
        }

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
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class)) continue;
            loadLoader(clazz);
            plugin.saveConfig();
        }
        log.info("Loaded {} Loader Achievements for {}", count, plugin.getName()).submit();
    }

    public void loadAllAchievements(Set<Class<? extends IAchievement>> classes) {
        count = 0;
        for (var clazz : classes) {
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            if(clazz.isAnnotationPresent(Deprecated.class) || clazz.isAnnotationPresent(NoReflection.class)) continue;
            load(clazz);
            plugin.saveConfig();
        }
        log.info("Loaded {} Achievements for {}", count, plugin.getName()).submit();
    }

    public void loadAll(Set<Class<? extends IAchievementCategory>> classes) {
        for (var clazz : classes) {
            if (IAchievementCategory.class.isAssignableFrom(clazz) && !clazz.isAnnotationPresent(SubCategory.class)) {
                if (!Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers())) {
                    loadCategory(clazz);
                }
            }
        }
    }

    public void loadSubCategories(Set<Class<?>> classes) {
        for (var clazz : classes) {
            SubCategory subCategoryAnnotation = clazz.getAnnotation(SubCategory.class);
            IAchievementCategory category = plugin.getInjector().getInstance(subCategoryAnnotation.value());
            IAchievementCategory subCategory = (IAchievementCategory) plugin.getInjector().getInstance(clazz);
            subCategory.setParent(category.getNamespacedKey());
            category.addChild(subCategory);
            plugin.getInjector().injectMembers(subCategory);
            achievementManager.getAchievementCategoryManager().addObject(subCategory.getNamespacedKey(), subCategory);
            log.info("Added {} to {} sub achievement categories", subCategory.getNamespacedKey().asString(), category.getNamespacedKey().asString()).submit();

        }
    }

    public void loadCategory(Class<?> clazz) {
        try {
            IAchievementCategory category = (IAchievementCategory) plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(category);
            achievementManager.getAchievementCategoryManager().addObject(category.getNamespacedKey(), category);
            count++;
        } catch (Exception ex) {
            log.error("Failed to load categoru", ex);
        }
    }

    public void loadAll(String packageName) {
        loadAchievementCategories(packageName);
        loadAchievements(packageName);

    }

    protected abstract void loadAchievementCategories(String packageName);

    protected abstract void loadAchievements(String packageName);
}
