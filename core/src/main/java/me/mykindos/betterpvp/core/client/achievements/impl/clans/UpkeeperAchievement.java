package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

// Config-loaded achievement. Loaded by UpkeeperAchievementLoader.
@CustomLog
@NoReflection
public class UpkeeperAchievement extends SingleSimpleAchievement {

    public UpkeeperAchievement(NamespacedKey key, int goal) {
        super("Upkeeper", key,
                AchievementCategories.CLANS_ENERGY_COLLECTED_TYPE,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        ClanWrapperStat.builder()
                                .wrappedStat(ClientStat.CLANS_ENERGY_COLLECTED)
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Collect " + getGoal() + " energy";
    }

    @Override
    public Description getDescription(StatContainer container, StatFilterType type, Period period) {
        List<Component> lore = new ArrayList<>(List.of(
                UtilMessage.deserialize("<gray>Collect <yellow>%s</yellow> energy to maintain your Clan's territory", getGoal())
        ));
        lore.addAll(this.getProgressComponent(container, type, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.AMETHYST_SHARD)
                .displayName(UtilMessage.deserialize("<white>%s", getName()))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
}

