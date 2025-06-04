package me.mykindos.betterpvp.champions.achievements.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategoryManager;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.loader.AchievementCategoryLoader;
import org.reflections.Reflections;
@Singleton
@CustomLog
public class ChampionsAchievementCategoryLoader extends AchievementCategoryLoader {
    @Inject
    public ChampionsAchievementCategoryLoader(Champions plugin, AchievementCategoryManager achievementCategoryManager) {
        super(plugin, achievementCategoryManager);
    }

    @Override
    public void loadAchievementCategories(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAchievementCategory>> classes = reflections.getSubTypesOf(IAchievementCategory.class);
        loadAll(classes);

        Set<Class<?>> subCategoryClasses = reflections.getTypesAnnotatedWith(SubCategory.class);
        loadSubCategories(subCategoryClasses);

        plugin.saveConfig();
        log.error("Loaded {} categories for Champions", count).submit();
    }
}
