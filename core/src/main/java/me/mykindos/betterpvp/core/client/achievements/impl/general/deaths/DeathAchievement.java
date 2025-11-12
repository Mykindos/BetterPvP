package me.mykindos.betterpvp.core.client.achievements.impl.general.deaths;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.core.MinecraftStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;

import java.util.ArrayList;
import java.util.List;

@CustomLog
//Config loaded achievement, this class will be skipped by reflaction
@NoReflection
/**
 * Super class, is either extended or loaded by a loader {@link DeathAchievementLoader}
 */
public class DeathAchievement extends SingleSimpleAchievement {

    public DeathAchievement(String key, int goal) {
        this(new NamespacedKey("core", key), goal);
    }

    public DeathAchievement(NamespacedKey key, int goal) {
        super("Death", key,
                AchievementCategories.DEATH_TYPE,
                AchievementType.PERIOD,
                (double) goal,
                new GenericStat(
                MinecraftStat.builder()
                        .statistic(Statistic.DEATHS)
                        .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Death " + getGoal().intValue();
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @return
     */
    @Override
    public Description getDescription(StatContainer container, String period) {
        List<Component> lore = new ArrayList<>(List.of(
            UtilMessage.deserialize("<gray>Die <yellow>%s</yellow> times", getGoal().intValue())
        ));
        lore.addAll(this.getProgressComponent(container, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.SKELETON_SKULL)
                .displayName(UtilMessage.deserialize("<white>%s", getName()))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }

    @Override
    public void processRewards(StatContainer container) {
        super.processRewards(container);
        //todo reimplement
//        RewardBox rewardBox = clientSQLLayer.getRewardBox(container.getUniqueId());
//        rewardBox.getContents().add(ItemStack.of(Material.SKELETON_SKULL));
//        clientSQLLayer.updateClientRewards(container.getUniqueId(), rewardBox);
    }
}