package me.mykindos.betterpvp.core.client.achievements.category.loader;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategoryManager;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;

import java.lang.reflect.Modifier;
import java.util.Set;

@CustomLog
public abstract class AchievementCategoryLoader extends Loader {
    private final AchievementCategoryManager achievementCategoryManager;
    protected AchievementCategoryLoader(BPvPPlugin plugin, AchievementCategoryManager achievementCategoryManager) {
        super(plugin);
        this.achievementCategoryManager = achievementCategoryManager;
    }

    public void loadAll(Set<Class<? extends IAchievementCategory>> classes) {
        for (var clazz : classes) {
            if (IAchievementCategory.class.isAssignableFrom(clazz) && !clazz.isAnnotationPresent(SubCategory.class)) {
                if (!Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers())) {
                    load(clazz);
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
            achievementCategoryManager.addObject(subCategory.getNamespacedKey(), subCategory);
            log.info("Added {} to {} sub achievement categories", subCategory.getNamespacedKey().asString(), category.getNamespacedKey().asString()).submit();

        }
    }

    @Override
    public void load(Class<?> clazz) {
        try {
            IAchievementCategory category = (IAchievementCategory) plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(category);
            achievementCategoryManager.addObject(category.getNamespacedKey(), category);
            log.info("loaded {}", category.getNamespacedKey().asString()).submit();
            count++;
        } catch (Exception ex) {
            log.error("Failed to load category", ex);
        }
    }

    public abstract void loadAchievementCategories(String packageName);
}
