package me.mykindos.betterpvp.champions.achievements.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.loader.AchievementLoader;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.IConfigAchievementLoader;
import org.reflections.Reflections;

import java.util.Set;
@Singleton
@CustomLog
public class ChampionsAchievementLoader extends AchievementLoader {
    @Inject
    public ChampionsAchievementLoader(Champions plugin, AchievementManager achievementManager) {
        super(plugin, achievementManager);
    }

    @Override
    protected void loadAchievements(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAchievement>> classes = reflections.getSubTypesOf(IAchievement.class);
        loadAllAchievements(classes);
        Set<Class<? extends IConfigAchievementLoader>> loaderClasses = reflections.getSubTypesOf(IConfigAchievementLoader.class);
        loadAllLoaderAchievements(loaderClasses);
    }

    @Override
    protected void loadAchievementCategories(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAchievementCategory>> classes = reflections.getSubTypesOf(IAchievementCategory.class);
        loadAll(classes);

        Set<Class<?>> subCategoryClasses = reflections.getTypesAnnotatedWith(SubCategory.class);
        loadSubCategories(subCategoryClasses);

        plugin.saveConfig();
        log.error("Loaded {} categories for Core", count).submit();
    }
}
