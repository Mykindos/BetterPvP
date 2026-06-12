package me.mykindos.betterpvp.core.client.achievements.impl.champions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class CastFireball extends SingleSimpleAchievement {
    @Inject
    public CastFireball() {
        super("I Cast Fireball",
                new NamespacedKey("champions", "cast_fireball_100"),
                AchievementCategories.CHAMPIONS,
                StatFilterType.ALL,
                100L,
                new GenericStat(
                        ChampionsSkillStat.builder()
                        .action(ChampionsSkillStat.Action.USE)
                        .skillName("Fire Blast")
                        .build()
                )
        );
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @param period
     * @return
     */
    @Override
    public Description getDescription(StatContainer container, StatFilterType type, Period period) {
        List<Component> lore = new ArrayList<>(
                List.of(
                        Translations.component("core.achievement.cast-fireball.desc",
                                Translations.component("champions.skill.mage.fire-blast.name").color(NamedTextColor.WHITE),
                                Component.text(100, NamedTextColor.YELLOW)).color(NamedTextColor.GOLD)
                ));
        lore.addAll(this.getProgressComponent(container, type, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.FIRE_CHARGE)
                .displayName(Translations.component("core.achievement.cast-fireball.name").color(NamedTextColor.WHITE))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
}
