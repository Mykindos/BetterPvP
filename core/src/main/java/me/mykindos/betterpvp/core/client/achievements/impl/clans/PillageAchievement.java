package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.server.Period;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
@BPvPListener
public class PillageAchievement extends SingleSimpleAchievement {
    @Inject
    public PillageAchievement() {
        super("ATTACK!",
                new NamespacedKey("clans", "pillage_attack"),
                AchievementCategories.CLANS,
                StatFilterType.ALL,
                1L,
                ClanWrapperStat.builder()
                        .wrappedStat(ClientStat.CLANS_ATTACK_PILLAGE)
                        .build()
        );
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.TNT;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("Pillage another Clan");
    }
}
