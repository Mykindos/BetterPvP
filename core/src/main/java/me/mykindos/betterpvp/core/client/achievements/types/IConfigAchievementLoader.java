package me.mykindos.betterpvp.core.client.achievements.types;

import java.util.Collection;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.NamespacedKey;

public interface IConfigAchievementLoader<T extends IAchievement> {
    Collection<T> loadAchievements(ExtendedYamlConfiguration config);
    NamespacedKey getTypeKey();

    default String getLoadPath(String path) {
        return getBasePath() + getTypeKey().asString() + "." + path;
    }
    default String getBasePath() {
        return "loadable.";
    }
}
