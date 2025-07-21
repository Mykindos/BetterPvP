package me.mykindos.betterpvp.core.client.achievements.types;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.IConfigAchievementLoader;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.stream.Collectors;

@CustomLog
public abstract class SingleSimpleAchievementConfigLoader<T extends SingleSimpleAchievement> implements IConfigAchievementLoader<T> {

    private final BPvPPlugin plugin;

    protected SingleSimpleAchievementConfigLoader(BPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(T achievement) {
        plugin.getListeners().add(achievement);
        Bukkit.getPluginManager().registerEvents(achievement, plugin);
    }

    @Override
    public Collection<T> loadAchievements(ExtendedYamlConfiguration config) {
        String path = getBasePath() + getTypeKey().asString();
        ConfigurationSection section = config.getOrCreateSection(path);
        return section.getKeys(false).stream()
                .map(name -> {
                    Number goal = config.getOrSaveObject(path + "." + name + ".goal", 5, Number.class);
                    T achievement = instanstiateAchievement(NamespacedKey.fromString(name), goal.doubleValue());
                    achievement.loadConfig(path + ".", config);
                    register(achievement);
                    return achievement;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Initialises the {@link IAchievement}, generally by calling {@code new}
     * @param key the {@link NamespacedKey} for this achievement
     * @param goal the goal of the achievement
     * @return the instantiated achievement
     */
    protected abstract T instanstiateAchievement(NamespacedKey key, Double goal);
}
