package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.fishingincreasedexperience;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.professions.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class FishingIncreasedExperienceAttributeListener implements Listener {

    private final FishingIncreasedExperienceAttribute attribute;

    @Inject
    public FishingIncreasedExperienceAttributeListener(FishingIncreasedExperienceAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGainExperience(PlayerProgressionExperienceEvent event) {
        if (!event.getProfession().equalsIgnoreCase("Fishing")) return;

        double increase = attribute.getExperienceIncrease(event.getPlayer());
        if (increase <= 0) return;

        event.setGainedExp(event.getGainedExp() * (1 + increase));
    }
}
