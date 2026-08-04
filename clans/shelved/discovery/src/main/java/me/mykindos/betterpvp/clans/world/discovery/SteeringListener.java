package me.mykindos.betterpvp.clans.world.discovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Turns held right-clicks on a steering control into steering input.
 * <p>
 * There is no packet for letting go of the button. The vanilla client re-sends this interact every few ticks for as
 * long as the button is down, which is what {@link SteeringInput}'s grace window reads as somebody still holding on —
 * the same trick the cannon uses to be aimed by holding the mouse.
 */
@BPvPListener
@Singleton
@PluginAdapter("ModelEngine")
public class SteeringListener implements Listener {

    private final SteeringService steering;

    @Inject
    public SteeringListener(SteeringService steering) {
        this.steering = steering;
    }

    @EventHandler
    public void onSteer(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // the off-hand copy of the same click would count as a second press
        }

        if (steering.press(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}
