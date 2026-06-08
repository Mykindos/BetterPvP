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
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

// Config-loaded achievement. Loaded by DemolisherAchievementLoader.
@CustomLog
@NoReflection
public class DemolisherAchievement extends SingleSimpleAchievement {

    public DemolisherAchievement(NamespacedKey key, int goal) {
        super("Demolisher", key,
                AchievementCategories.CLANS_CANNON_BLOCK_DAMAGE_TYPE,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        ClanWrapperStat.builder()
                                .wrappedStat(ClientStat.CLANS_CANNON_BLOCK_DAMAGE)
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Deal " + getGoal() + " cannon block damage";
    }

    @Override
    public Description getDescription(StatContainer container, StatFilterType type, Period period) {
        List<Component> lore = new ArrayList<>(List.of(
                Translations.component("core.achievement.demolisher.desc",
                        Component.text(String.valueOf(getGoal()), NamedTextColor.YELLOW))
        ));
        lore.addAll(this.getProgressComponent(container, type, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.TNT)
                .displayName(Translations.component("core.achievement.demolisher.name", Component.text(String.valueOf(getGoal()))).color(NamedTextColor.WHITE))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
}
