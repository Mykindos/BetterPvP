package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.barkmultiplier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.barkchance.BarkChanceAttributeListener.BarkDropEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class BarkMultiplierAttributeListener implements Listener {

    private final WoodcuttingBarkMultiplierAttribute attribute;

    @Inject
    public BarkMultiplierAttributeListener(WoodcuttingBarkMultiplierAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler
    public void onBarkDrop(BarkDropEvent event) {
        double chance = attribute.getChance(event.getPlayer());
        if (chance <= 0) return;
        if (chance > 1 ? Math.random() * 100.0 >= chance : Math.random() >= chance) return;

        event.getItemStack().setAmount(event.getItemStack().getAmount() * 2);
    }
}
