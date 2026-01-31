package me.mykindos.betterpvp.core.block.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.interaction.event.InteractionPreExecuteEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.RayTraceResult;

import java.util.Objects;

@BPvPListener
@Singleton
public class SmartBlockInteractionListener implements Listener {

    private final SmartBlockFactory factory;

    @Inject
    private SmartBlockInteractionListener(SmartBlockFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onInteractionPreUse(InteractionPreExecuteEvent event) {
        if (!event.getActor().isPlayer()) {
            return;
        }
        final Player player = (Player) event.getActor().getEntity();
        final double blockReach = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)).getValue();
        final RayTraceResult blockResult = player.rayTraceBlocks(blockReach);
        if (blockResult != null) {
            final Location location = blockResult.getHitPosition().toLocation(player.getWorld());
            if (factory.isSmartBlock(location)) {
                event.setCancelled(true);
            }
            return;
        }

        final double entityReach = Objects.requireNonNull(player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)).getValue();
        final RayTraceResult entityResult = player.rayTraceEntities((int) entityReach);
        if (entityResult != null) {
            final Location location = entityResult.getHitPosition().toLocation(player.getWorld());
            if (factory.isSmartBlock(location)) {
                event.setCancelled(true);
            }
        }
    }
}
