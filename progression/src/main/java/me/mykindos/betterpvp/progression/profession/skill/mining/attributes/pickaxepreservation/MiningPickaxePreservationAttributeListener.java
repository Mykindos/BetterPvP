package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.pickaxepreservation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class MiningPickaxePreservationAttributeListener implements Listener {

    private final MiningPickaxePreservationAttribute attribute;

    @Inject
    public MiningPickaxePreservationAttributeListener(MiningPickaxePreservationAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (!Tag.ITEMS_PICKAXES.isTagged(item.getType())) return;

        double chance = attribute.getPreservationChance(event.getPlayer());
        if (chance <= 0) return;
        if (Math.random() >= chance) return;

        event.setCancelled(true);
    }
}
