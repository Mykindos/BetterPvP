package me.mykindos.betterpvp.core.client.achievements.test.gamer.deaths;

import java.util.List;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.achievements.types.containertypes.IGamerAchievement;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.ConfigLoadedAchievement;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.rewards.RewardBox;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@CustomLog
//Config loaded achievement, this class will be skipped by reflaction
@ConfigLoadedAchievement
/**
 * Super class, is either extended or loaded by a loader {@link DeathAchievementLoader}
 */
public class DeathAchievement extends SingleSimpleAchievement<Gamer, GamerPropertyUpdateEvent, Integer> implements IGamerAchievement {

    public DeathAchievement(String key, int goal) {
        this(new NamespacedKey("core", key), goal);
    }

    public DeathAchievement(NamespacedKey key, int goal) {
        super(key, AchievementCategories.DEATH_TYPE, goal, GamerProperty.DEATHS);
    }

    @Override
    public String getName() {
        return "Death " + goal;
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @return
     */
    @Override
    public Description getDescription(Gamer container) {
        List<Component> lore = new java.util.ArrayList<>(List.of(
            UtilMessage.deserialize("<gray>Die <yellow>%s</yellow> times", goal)
        ));
        lore.addAll(this.getProgressComponent(container));
        lore.addAll(this.getCompletionComponent(container));
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.BOOK)
                .displayName(UtilMessage.deserialize("<white>%s", getName()))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }

    @Override
    public void processRewards(Gamer container) {
        super.processRewards(container);
        RewardBox rewardBox = clientSQLLayer.getRewardBox(container.getUniqueId());
        rewardBox.getContents().add(ItemStack.of(Material.SKELETON_SKULL));
        clientSQLLayer.updateClientRewards(container.getUniqueId(), rewardBox);
    }
}