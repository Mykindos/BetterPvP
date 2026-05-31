package me.mykindos.betterpvp.core.world.zone;

import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.block.Container;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link ZoneRule} that protects a zone from modification: block breaking and placing are denied, and container
 * access is denied, while harmless interactions (doors, buttons, crafting/enchanting tables, ...) are left to default.
 * This is the composable "no build" capability that replaced the old admin-clan build protection; attach it to any
 * zone (server safe zones, spawn, shops) that should be read-only to players.
 * <p>
 * Players who are {@link me.mykindos.betterpvp.core.client.Client#isAdministrating() administrating} bypass the rule
 * so staff can still build the area.
 */
public final class NoBuildRule implements ZoneRule {

    private final ClientManager clientManager;

    public NoBuildRule(@NotNull ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull Event.Result evaluate(@NotNull ZoneActionContext context) {
        if (clientManager.search().online(context.getPlayer()).isAdministrating()) {
            return Event.Result.DEFAULT;
        }
        return switch (context.getInteraction()) {
            case BREAK, PLACE -> Event.Result.DENY;
            case INTERACT -> context.getBlock() != null && context.getBlock().getState() instanceof Container
                    ? Event.Result.DENY
                    : Event.Result.DEFAULT;
            default -> Event.Result.DEFAULT;
        };
    }
}
