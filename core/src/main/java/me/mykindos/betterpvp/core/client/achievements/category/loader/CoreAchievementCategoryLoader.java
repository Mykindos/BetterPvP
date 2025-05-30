package me.mykindos.betterpvp.core.client.achievements.category.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategoryManager;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.command.SubCommand;
import org.reflections.Reflections;

@Singleton
@CustomLog
public class CoreAchievementCategoryLoader extends AchievementCategoryLoader {
    @Inject
    public CoreAchievementCategoryLoader(Core plugin, AchievementCategoryManager achievementCategoryManager) {
        super(plugin, achievementCategoryManager);
    }


    @Override
    public void loadAchievementCategories(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAchievementCategory>> classes = reflections.getSubTypesOf(IAchievementCategory.class);
        loadAll(classes);

        Set<Class<?>> subCommandClasses = reflections.getTypesAnnotatedWith(SubCommand.class);
        loadSubCategories(subCommandClasses);

        plugin.saveConfig();
        log.info("Loaded {} categories for Core", count).submit();
    }
}
