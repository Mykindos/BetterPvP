package me.mykindos.betterpvp.core.client.achievements.test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class DeathAchievementLoader extends SingleSimpleAchievementConfigLoader<DeathAchievement> {
    @Inject
    public DeathAchievementLoader() {
    }

    @Override
    public NamespacedKey getTypeKey() {
        return new NamespacedKey("core", "deaths");
    }

}
