package me.mykindos.betterpvp.core.client.achievements.test;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.types.ConfigLoadedAchievement;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@CustomLog
@Singleton
@ConfigLoadedAchievement
public class DeathAchievement extends SingleSimpleAchievement<Gamer, GamerPropertyUpdateEvent, Integer> {
    public DeathAchievement(String key, int goal) {
        super(new NamespacedKey("core", key), goal, GamerProperty.DEATHS);
    }

    @Override
    public void onChangeValue(Gamer container, String property, Object newValue, Object oldValue, Map<String, Object> otherProperties) {
        //todo do better
        int current = getProperty(container);
        UtilMessage.message(Objects.requireNonNull(container.getPlayer()), UtilMessage.deserialize("Progress: <green>%s</green>/<yellow>%s</yellow> (<green>%s</green>%%)", current, goal, getPercentComplete(container) * 100));
        Optional<AchievementCompletion> achievementCompletionOptional = getAchievementCompletion(container);
        if (current >= goal) {
            if (achievementCompletionOptional.isEmpty()) {
                //todo make more descriptionalble'

                UtilMessage.message(container.getPlayer(), "Achievement", "You completed this achievement!");
                complete(container);
            } else {
                final AchievementCompletion achievementCompletion = achievementCompletionOptional.get();
                UtilMessage.message(container.getPlayer(), "Achievement", "You completed this achievement <green>%s ago", UtilTime.getTime(System.currentTimeMillis() - achievementCompletion.getTimestamp().getTime(), 1));
            }

        }
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
        final int current = getProperty(container);
        ItemProvider itemProvider = ItemView.builder()
                .material(Material.BOOK)
                .displayName(UtilMessage.deserialize("Death %s", goal))
                .lore(UtilMessage.deserialize("Progress: <green>%s</green>/<yellow>%s</yellow> (<green>%s</green>%%)", current, goal, getPercentComplete(container) * 100))
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }
}
