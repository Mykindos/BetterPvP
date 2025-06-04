package me.mykindos.betterpvp.champions.achievements.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.loader.AchievementLoader;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.IConfigAchievementLoader;
import org.reflections.Reflections;
@Singleton
public class ChampionsAchievementLoader extends AchievementLoader {
    @Inject
    public ChampionsAchievementLoader(Champions plugin, AchievementManager achievementManager) {
        super(plugin, achievementManager);
    }

    @Override
    public void loadAchievements(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAchievement>> classes = reflections.getSubTypesOf(IAchievement.class);
        loadAllAchievements(classes);
        Set<Class<? extends IConfigAchievementLoader>> loaderClasses = reflections.getSubTypesOf(IConfigAchievementLoader.class);
        loadAllLoaderAchievements(loaderClasses);
    }
}
