package me.mykindos.betterpvp.core.npc.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

@BPvPListener
@Singleton
public class NPCInteractListener implements Listener {

    private final NPCRegistry registry;

    @Inject
    private NPCInteractListener(NPCRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!EquipmentSlot.HAND.equals(event.getHand())) return; // not the main hand
        final NPC npc = registry.getNPC(event.getRightClicked());
        if (npc == null) return; // non existent
        event.setCancelled(true);
        npc.act(event.getPlayer());
    }

}
