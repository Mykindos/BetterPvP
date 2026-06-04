package me.mykindos.betterpvp.clans.world.resource;

import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.world.zone.ZoneActionContext;
import me.mykindos.betterpvp.core.world.zone.ZoneRule;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Denies ordinary block break/interaction inside a resource-node zone so that {@link ResourceNodeManager} can take
 * over the interaction and run the archetype's harvest (this mirrors how the Fields zone denied breaking and let the
 * Fields listener perform the mine). Administrating players bypass it, so they can build the nodes in-world.
 */
public class ResourceNodeRule implements ZoneRule {

    private final ClientManager clientManager;

    public ResourceNodeRule(@NotNull ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull Event.Result evaluate(@NotNull ZoneActionContext context) {
        if (clientManager.search().online(context.getPlayer()).isAdministrating()) {
            return Event.Result.DEFAULT;
        }
        return switch (context.getInteraction()) {
            case BREAK, INTERACT -> Event.Result.DENY;
            default -> Event.Result.DEFAULT;
        };
    }
}
