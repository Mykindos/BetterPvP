package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
public class TeleportHomeAchievement extends SingleSimpleAchievement {
    @Inject
    public TeleportHomeAchievement() {
        super("Home Sweet Home",
                new NamespacedKey("clans", "teleport_core"),
                AchievementCategories.CLANS,
                AchievementType.GLOBAL,
                1d,
                ClanWrapperStat.builder()
                        .wrappedStat(ClientStat.CLANS_TELEPORT_CORE)
                        .build()
        );
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.RESPAWN_ANCHOR;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of("Teleport to your Clan's core");
    }
}
