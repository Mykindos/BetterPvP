package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.baitduration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class BaitDurationAttributeListener implements Listener {

    private final BaitDurationAttribute attribute;

    @Inject
    public BaitDurationAttributeListener(BaitDurationAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler
    public void onThrowBait(PlayerThrowBaitEvent event) {
        double bonus = attribute.getBaitDurationBonus(event.getPlayer());
        if (bonus <= 0) return;

        event.getBait().setDurationInTicks((long) (event.getBait().getDurationInTicks() * (1 + bonus)));
    }
}
