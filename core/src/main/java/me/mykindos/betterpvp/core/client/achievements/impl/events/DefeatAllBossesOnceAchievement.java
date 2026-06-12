package me.mykindos.betterpvp.core.client.achievements.impl.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.NSingleGoalSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.events.BossStat;
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
public class DefeatAllBossesOnceAchievement extends NSingleGoalSimpleAchievement {
    @Inject
    public DefeatAllBossesOnceAchievement() {
        super("Defeat All Bosses",
                new NamespacedKey("events", "defeat_all_bosses"),
                AchievementCategories.EVENT,
                StatFilterType.ALL,
                1L,
                new GenericStat(
                        BossStat.builder()
                        .action(BossStat.Action.KILL)
                        .bossName("Skeleton King")
                        .build()
                ),
                new GenericStat(
                        BossStat.builder()
                        .action(BossStat.Action.KILL)
                        .bossName("Dreadbeard")
                        .build()
                ),
                new GenericStat(
                        BossStat.builder()
                        .action(BossStat.Action.KILL)
                        .bossName("Deep Creature")
                        .build()
                ),
                new GenericStat(
                        BossStat.builder()
                        .action(BossStat.Action.KILL)
                        .bossName("Zanzul")
                        .build()
                )
        );
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @return
     */
    @Override
    public Description getDescription(StatContainer container, StatFilterType type, Period period) {
        List<Component> lore = new ArrayList<>(
                List.of(
                    Translations.component("core.achievement.defeat-all-bosses.desc").color(NamedTextColor.GRAY)
        ));
        lore.addAll(this.getProgressComponent(container, type, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.BEACON)
                .displayName(Translations.component("core.achievement.defeat-all-bosses.name").color(NamedTextColor.WHITE))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
    @Override
    public List<Component> getProgressComponent(StatContainer container, StatFilterType type, Period period) {
        final GenericStat skeletonKingStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Skeleton King")
                .build()
        );
        final GenericStat dreadbeardStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Dreadbeard")
                .build()
        );
        final GenericStat deepCreatureStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Deep Creature")
                .build()
        );
        final GenericStat zanzulStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Zanzul")
                .build()
        );
        boolean killedSkeletonKing = skeletonKingStat.getStat(container, type, period) >= 1;
        List<Component> bar = super.getProgressComponent(container, type, period);
        bar.addAll(List.of(
                        Translations.component("core.achievement.defeat-all-bosses.boss-progress",
                                Translations.component("core.achievement.category.dreadbeard.name").color(NamedTextColor.WHITE),
                                Component.text(String.valueOf(dreadbeardStat.getStat(container, type, period)), NamedTextColor.GREEN),
                                Component.text(String.valueOf(statGoals.get(dreadbeardStat)), NamedTextColor.YELLOW)),
                        Translations.component("core.achievement.defeat-all-bosses.boss-progress",
                                (killedSkeletonKing
                                        ? Translations.component("core.achievement.category.skeleton-king.name")
                                        : Translations.component("core.achievement.defeat-all-bosses.hidden-boss")).color(NamedTextColor.WHITE),
                                Component.text(String.valueOf(skeletonKingStat.getStat(container, type, period)), NamedTextColor.GREEN),
                                Component.text(String.valueOf(statGoals.get(skeletonKingStat)), NamedTextColor.YELLOW)),
                        Translations.component("core.achievement.defeat-all-bosses.boss-progress",
                                Translations.component("core.achievement.category.deep-creature.name").color(NamedTextColor.WHITE),
                                Component.text(String.valueOf(deepCreatureStat.getStat(container, type, period)), NamedTextColor.GREEN),
                                Component.text(String.valueOf(statGoals.get(deepCreatureStat)), NamedTextColor.YELLOW)),
                        Translations.component("core.achievement.defeat-all-bosses.boss-progress",
                                Translations.component("core.achievement.category.zanzul.name").color(NamedTextColor.WHITE),
                                Component.text(String.valueOf(zanzulStat.getStat(container, type, period)), NamedTextColor.GREEN),
                                Component.text(String.valueOf(statGoals.get(zanzulStat)), NamedTextColor.YELLOW))
                        )
        );
        return bar;
    }

    @Override
    public String getName() {
        return "Defeat All Bosses";
    }
}
