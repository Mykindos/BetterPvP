package me.mykindos.betterpvp.core.loot.chest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.events.MythicMobInteractEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
@PluginAdapter("MythicMobs")
public class LootChestListener implements Listener {

    private final LootChestManager lootChestManager;

    @Inject
    public LootChestListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChestDamage(DamageEvent event) {
        final LootChest lootChest = lootChestManager.getLootChest(event.getDamagee());
        if (lootChest != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVelocity(CustomEntityVelocityEvent event) {
        final LootChest lootChest = lootChestManager.getLootChest(event.getEntity());
        if (lootChest != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKnockback(CustomKnockbackEvent event) {
        final LootChest lootChest = lootChestManager.getLootChest(event.getDamagee());
        if (lootChest != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKnockback(EntityCanHurtEntityEvent event) {
        final LootChest lootChest = lootChestManager.getLootChest(event.getDamagee());
        if (lootChest != null) {
            event.setResult(Event.Result.DENY);
        }
    }

    // Grant awards
    @EventHandler
    public void onChestInteract(MythicMobInteractEvent event) {
        if (event.isCancelled()) return;

        LootChest lootChest = lootChestManager.getLootChest(event.getActiveMob());
        if (lootChest != null) {
            lootChest.dropItems();
            lootChestManager.getLootChests().remove(lootChest);
        }
    }

    // Remove invalid loot chests every 1 second
    @UpdateEvent (delay = 1000)
    public void removeInvalidLootChests() {
        lootChestManager.getLootChests().removeIf(lootChest -> {
            return lootChest.getActiveMob().isDead() || !lootChest.getActiveMob().getEntity().getBukkitEntity().isValid();
        });
    }
}
