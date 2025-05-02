package me.mykindos.betterpvp.core.client.achievements.test;

import java.util.Map;
import java.util.Objects;
import me.mykindos.betterpvp.core.client.achievements.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;

public abstract class DeathAchievement extends SingleSimpleAchievement<Gamer, GamerPropertyUpdateEvent, Integer> {
    public DeathAchievement(String name, int goal) {
        super(name, goal, GamerProperty.DEATHS);
    }

    @Override
    public void onChangeValue(Gamer container, String property, Object newValue, Object oldValue, Map<String, Object> otherProperties) {
        //todo do better
        int current = getProperty(container);
        UtilMessage.message(Objects.requireNonNull(container.getPlayer()), UtilMessage.deserialize("Progress: <green>%s</green>/<yellow>%s</yellow> (<green>%s</green>%%)", current, goal, getPercentComplete(container) * 100));
    }


}
