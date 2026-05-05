package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.baitnonconsumptionchance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class BaitNonConsumptionChanceAttributeListener implements Listener {

    private final BaitNonConsumptionChanceAttribute attribute;

    @Inject
    public BaitNonConsumptionChanceAttributeListener(BaitNonConsumptionChanceAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler
    public void onThrowBait(PlayerThrowBaitEvent event) {
        double chance = attribute.getNonConsumptionChance(event.getPlayer());
        if (chance <= 0) return;
        if (Math.random() >= chance) return;

        // BaitAbility already removed the bait item before firing this event; give it back.
        Player player = event.getPlayer();
        player.getInventory().addItem(new ItemStack(event.getBait().getMaterial(), 1));
    }
}
