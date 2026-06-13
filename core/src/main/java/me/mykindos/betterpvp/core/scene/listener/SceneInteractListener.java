package me.mykindos.betterpvp.core.scene.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectInteractEvent;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;

import java.util.Objects;

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
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!EquipmentSlot.HAND.equals(event.getHand())) return;
        final SceneObject obj = registry.getObject(event.getRightClicked());
        if (obj == null) return;
        event.setCancelled(true);
        // Funnel real-entity interactions through the same seam packet-only objects use, so all
        // downstream listeners (act dispatch below, quest-givers, etc.) see one event type.
        UtilServer.callEvent(new SceneObjectInteractEvent(event.getPlayer(), obj));
    }

    @EventHandler
    public void onSceneInteract(SceneObjectInteractEvent event) {
        if (event.getSceneObject() instanceof Actor actor) {
            actor.act(event.getPlayer());
        }
    }

    // Cancel the event so click an NPC only does the NPC's action
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isLookingAtNPC(event.getPlayer())) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    private boolean isLookingAtNPC(Player player) {
        final double reach = Objects.requireNonNull(player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)).getValue();
        final RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getLocation().getDirection(),
                reach,
                entity -> registry.getObject(entity) instanceof Actor);
        return rayTraceResult != null;
    }

}
