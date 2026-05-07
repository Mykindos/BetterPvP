package me.mykindos.betterpvp.core.client.achievements.impl.general.damagedealt;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.core.DamageStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Type;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
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

@CustomLog
//Config loaded achievement, this class will be skipped by reflection
@NoReflection
/**
 * Super class, is either extended or loaded by a loader {@link DamageDealtAchievementLoader}
 */
public class DamageDealtAchievement extends SingleSimpleAchievement {

    public DamageDealtAchievement(String key, double goal) {
        this(new NamespacedKey("core", key), goal);
    }

    public DamageDealtAchievement(NamespacedKey key, double goal) {
        super("Damage Dealt", key,
                AchievementCategories.DAMAGE_DEALT_TYPE,
                StatFilterType.SEASON,
                IStat.getLongValueOfDouble(goal),
                new GenericStat(
                        DamageStat.builder()
                                .relation(Relation.DEALT)
                                .type(Type.AMOUNT)
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Deal " + IStat.getDoubleValueOfLong(getGoal()) + " damage";
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
        List<Component> lore = new ArrayList<>(List.of(
                UtilMessage.deserialize("<gray>Deal <yellow>%s</yellow> damage", IStat.getDoubleValueOfLong(getGoal()))
        ));
        lore.addAll(this.getProgressComponent(container, type, period));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.IRON_SWORD)
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
//        rewardBox.getContents().add(ItemStack.of(Material.IRON_SWORD));
//        clientSQLLayer.updateClientRewards(container.getUniqueId(), rewardBox);
    }
}

