package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.catchweight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class CatchWeightAttributeListener implements Listener {

    private final CatchWeightAttribute attribute;

    @Inject
    public CatchWeightAttributeListener(CatchWeightAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatchFish(PlayerCaughtFishEvent event) {
        if (event.getFishLoot() == null) return;
        final var fish = event.getFishLoot().getCurrentFish();
        if (fish == null) return;

        double bonus = attribute.getCatchWeightBonus(event.getPlayer());
        if (bonus <= 0) return;

        fish.setWeight((int) (fish.getWeight() * (1 + bonus)));
    }
}
