package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
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

// Config-loaded achievement. Loaded by CoreBreakerAchievementLoader.
@CustomLog
@NoReflection
public class CoreBreakerAchievement extends SingleSimpleAchievement {

    public CoreBreakerAchievement(NamespacedKey key, double goal) {
        super("Core Breaker", key,
                AchievementCategories.CLANS_CORE_DAMAGE_TYPE,
                StatFilterType.SEASON,
                IStat.getLongValueOfDouble(goal),
                new GenericStat(
                        ClanWrapperStat.builder()
                                .wrappedStat(ClientStat.CLANS_CORE_DAMAGE)
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Deal " + IStat.getDoubleValueOfLong(getGoal()) + " core damage";
    }

    @Override
    public Description getDescription(StatContainer container, StatFilterType type, Period period) {
        List<Component> lore = new ArrayList<>(List.of(
                UtilMessage.deserialize("<gray>Deal <yellow>%s</yellow> damage to enemy Clan cores", IStat.getDoubleValueOfLong(getGoal()))
        ));
        lore.addAll(this.getProgressComponent(container, type, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.BEACON)
                .displayName(UtilMessage.deserialize("<white>%s", getName()))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
}
