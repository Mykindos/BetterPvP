package me.mykindos.betterpvp.core.client.achievements.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.AchievementManager;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import org.reflections.Reflections;

@Singleton
public class CoreAchievementLoader extends AchievementLoader {
    @Inject
    public CoreAchievementLoader(Core plugin, AchievementManager achievementManager) {
        super(plugin, achievementManager);
    }

    @Override
    public void loadAchievements(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAchievement>> classes = reflections.getSubTypesOf(IAchievement.class);
        loadAll(classes);
    }
}
