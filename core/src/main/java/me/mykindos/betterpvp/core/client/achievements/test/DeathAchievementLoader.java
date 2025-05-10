package me.mykindos.betterpvp.core.client.achievements.test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class DeathAchievementLoader extends SingleSimpleAchievementConfigLoader<DeathAchievement> {
    @Inject
    public DeathAchievementLoader(Core core) {
        super(core);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return new NamespacedKey("core", "deaths");
    }

    @Override
    protected DeathAchievement loadAchievement(String basePath, ExtendedYamlConfiguration config,  String namespacedKey) {
        NamespacedKey key = NamespacedKey.fromString(namespacedKey);
        int goal = config.getOrSaveInt(basePath + namespacedKey + ".goal", 5);
        DeathAchievement achievement = new DeathAchievement(Objects.requireNonNull(key).getKey(), goal);
        achievement.loadConfig(basePath, config);
        return achievement;
    }
}
