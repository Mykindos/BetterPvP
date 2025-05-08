package me.mykindos.betterpvp.core.client.achievements.test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.mykindos.betterpvp.core.client.achievements.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public abstract class MobKillsAchievement extends SingleSimpleAchievement<Gamer, GamerPropertyUpdateEvent, Integer> {
    //todo abstract common things to a simple achievement

    public MobKillsAchievement(String key, int goal) {
        super(new NamespacedKey("core", key), goal, GamerProperty.MOB_KILLS);
    }

    @Override
    public void onChangeValue(Gamer container, String property, Object newValue, Object oldValue, Map<String, Object> otherProperties) {
        //todo do better
        int current = getProperty(container);
        UtilMessage.message(Objects.requireNonNull(container.getPlayer()), UtilMessage.deserialize("Progress: <green>%s</green>/<yellow>%s</yellow> (<green>%s</green>%%)", current, goal, getPercentComplete(container) * 100));
    }

    @Override
    public Description getDescription(Gamer container) {
        int current = getProperty(container);
        ItemView itemView = ItemView.builder()
                .material(Material.ZOMBIE_SPAWN_EGG)
                .displayName(UtilMessage.deserialize("<light_purple>Kill <yellow>%s</yellow> Mobs", this.goal))
                .lore(List.of(UtilMessage.deserialize("Progress: <green>%s</green>/<yellow>%s</yellow> (<green>%s</green>%%)", current, goal, getPercentComplete(container) * 100)))
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }
}
