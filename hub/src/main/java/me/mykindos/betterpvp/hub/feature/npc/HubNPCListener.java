package me.mykindos.betterpvp.hub.feature.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Ticked;
import me.mykindos.betterpvp.hub.Hub;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

@BPvPListener
@Singleton
public class HubNPCListener implements Listener {

    @Inject
    private NPCRegistry registry;

    @Inject
    private HubNPCFactory factory;

    @Inject
    private Hub hub;

    private boolean loaded;

    @EventHandler
    public void onRegister(ModelRegistrationEvent event) {
        if (loaded) {
            return;
        }

        UtilServer.runTask(hub, () -> {
            loaded = factory.tryLoad(hub);
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!EquipmentSlot.HAND.equals(event.getHand())) {
            return; // not the main hand
        }

        final NPC found = this.registry.getNPC(event.getRightClicked());
        if (!(found instanceof HubNPC npc)) {
            return; // not a hub npc
        }

        npc.act(event.getPlayer());
    }

    @UpdateEvent
    public void tick() {
        this.registry.getNPCs().stream()
                .filter(HubNPC.class::isInstance)
                .filter(Ticked.class::isInstance)
                .map(Ticked.class::cast)
                .forEach(Ticked::tick);
    }

}
