package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.increasedexperience;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.professions.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class WoodcuttingIncreasedExperienceAttributeListener implements Listener {

    private final WoodcuttingIncreasedExperienceAttribute attribute;

    @Inject
    public WoodcuttingIncreasedExperienceAttributeListener(WoodcuttingIncreasedExperienceAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGainExperience(PlayerProgressionExperienceEvent event) {
        if (!event.getProfession().equalsIgnoreCase("Woodcutting")) return;

        double increase = attribute.getExperienceIncrease(event.getPlayer());
        if (increase <= 0) return;

        event.setGainedExp(event.getGainedExp() * (1 + increase));
    }
}
