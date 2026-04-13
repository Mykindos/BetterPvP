package me.mykindos.betterpvp.core.scene.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Forwards player right-click events to any {@link SceneObject} that implements {@link Actor}.
 * <p>
 * Replaces the old {@code NPCInteractListener}, which was limited to NPC objects.
 * Any registered scene object (NPC or otherwise) that opts into interaction by implementing
 * {@link Actor} will receive the event via {@link Actor#act(org.bukkit.entity.Player)}.
 */
@BPvPListener
@Singleton
public class SceneInteractListener implements Listener {

    private final SceneObjectRegistry registry;

    @Inject
    private SceneInteractListener(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!EquipmentSlot.HAND.equals(event.getHand())) return;
        final SceneObject obj = registry.getObject(event.getRightClicked());
        if (!(obj instanceof Actor actor)) return;
        event.setCancelled(true);
        actor.act(event.getPlayer());
    }

}
