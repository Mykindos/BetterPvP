package me.mykindos.betterpvp.core.client.achievements.test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.types.IConfigAchievementLoader;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class DeathAchievementLoader implements IConfigAchievementLoader<DeathAchievement> {
    @Inject
    public DeathAchievementLoader() {
    }

    @Override
    public Collection<DeathAchievement> loadAchievements(ExtendedYamlConfiguration config) {
        log.error(getTypeKey().asString()).submit();
        ExtendedYamlConfiguration section = config.getOrCreateSection(getTypeKey().asString());
        log.error(section.getValues(true).toString()).submit();
        //todo insantiate and load configs
        //todo have a seperate loadable config
        return Set.of();
    }

    @Override
    public NamespacedKey getTypeKey() {
        return null;
    }
}
