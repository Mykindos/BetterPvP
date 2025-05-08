package me.mykindos.betterpvp.core.client.achievements.test;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import me.mykindos.betterpvp.core.client.achievements.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.NamespacedKey;

public abstract class DeathAchievement extends SingleSimpleAchievement<Gamer, GamerPropertyUpdateEvent, Integer> {
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


}
