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
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
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
                    UtilMessage.deserialize("<gray>Kill all of the following Bosses:")
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
        boolean killedSkeletonKing = skeletonKingStat.getStat(container, type, period) >= 1;
        List<Component> bar = super.getProgressComponent(container, type, period);
        bar.addAll(List.of(
                        UtilMessage.deserialize("<white>Dreadbeard</white>: (<green>%s</green>/<yellow>%s</yellow>)",
                                dreadbeardStat.getStat(container, type, period),
                                statGoals.get(dreadbeardStat)),
                        UtilMessage.deserialize("<white>%s</white>: (<green>%s</green>/<yellow>%s</yellow>)",
                                killedSkeletonKing ? "Skeleton King" : "???",
                                skeletonKingStat.getStat(container, type, period),
                                statGoals.get(skeletonKingStat))
                        )
        );
        return bar;
    }

    @Override
    public String getName() {
        return "Defeat All Bosses";
    }
}
