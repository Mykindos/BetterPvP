package me.mykindos.betterpvp.core.client.achievements.types.loaded;

import java.util.Collection;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.NamespacedKey;

public interface IConfigAchievementLoader<T extends IAchievement> {
    /**
     * Registers this {@link IAchievement} as a listener
     * @param achievement the {@link IAchievement} to register
     */
    void register(T achievement);

    Collection<T> loadAchievements(ExtendedYamlConfiguration config);
    NamespacedKey getTypeKey();

    default String getLoadPath(String path) {
        return getBasePath() + getTypeKey().asString() + "." + path;
    }
    default String getBasePath() {
        return "loadable.";
    }
}
