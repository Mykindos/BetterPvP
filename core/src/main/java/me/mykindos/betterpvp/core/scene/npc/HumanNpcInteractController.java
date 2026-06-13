package me.mykindos.betterpvp.core.scene.npc;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.scene.SceneObjectInteractEvent;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bridges client right-clicks on packet-only {@link HumanNPC}s into the scene interaction seam.
 * <p>
 * These NPCs have no entity in the world, so Bukkit's {@code PlayerInteractEntityEvent} never
 * fires for them. We instead read the raw {@code INTERACT_ENTITY} packet, match its entity id to
 * a registered {@link HumanNPC}, and fire a {@link SceneObjectInteractEvent} on the main thread —
 * the same event real-entity scene objects fire — so downstream listeners are agnostic to how the
 * object is rendered. Real-entity scene objects are ignored here (they are not {@link HumanNPC}s),
 * so they keep flowing through the Bukkit path and never double-fire.
 */
public class HumanNpcInteractController implements PacketListener {

    private final SceneObjectRegistry registry;

    public HumanNpcInteractController(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        // INTERACT is the right-click action; gate to the main hand so a click fires exactly once.
        if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT
                || packet.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        HumanNPC npc = null;
        for (HumanNPC candidate : registry.getObjects(HumanNPC.class)) {
            if (candidate.getEntity().getEntityId() == packet.getEntityId()) {
                npc = candidate;
                break;
            }
        }
        if (npc == null) {
            return;
        }

        final HumanNPC clicked = npc;
        final Player player = (Player) event.getPlayer();
        UtilServer.runTask(JavaPlugin.getPlugin(Core.class),
                () -> UtilServer.callEvent(new SceneObjectInteractEvent(player, clicked)));
    }
}
