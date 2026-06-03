package me.mykindos.betterpvp.core.command.commands.admin.zone;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.Zone;
import org.bukkit.entity.Player;

/**
 * {@code /zone discovery remove <player> <zoneKey>} — removes a single discovery so re-entering the zone re-triggers
 * the notification.
 */
@Singleton
@SubCommand(ZoneDiscoverySubCommand.class)
public class ZoneDiscoveryRemoveSubCommand extends AbstractZoneDiscoverySubCommand {

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Remove a single zone discovery from a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final String[] stripped = stripName(args);
        if (stripped.length < 2) {
            UtilMessage.message(player, "Zones", "<yellow>Usage:</yellow> /zone discovery remove <player> <zoneKey>");
            return;
        }

        final String targetName = stripped[0];
        final Zone zone = resolveZone(stripped[1]);
        if (zone == null) {
            UtilMessage.message(player, "Zones", "<red>Unknown zone <yellow>%s</yellow>.", stripped[1]);
            return;
        }

        clientManager.search(player).offline(targetName).thenAccept(result -> {
            if (result.isEmpty()) {
                UtilServer.runTask(core, () -> UtilMessage.message(player, "Zones", "<red>Player <yellow>%s</yellow> was not found.", targetName));
                return;
            }
            final Client target = result.get();
            discoveryService.remove(target, zone).thenRun(() -> UtilServer.runTask(core, () ->
                    UtilMessage.message(player, "Zones", "<green>Removed</green> discovery <white>%s</white> from <yellow>%s</yellow>.",
                            zone.getKey().asString(), target.getName())));
        });
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }
        if (argCount == 2) {
            return zoneArgType();
        }
        return ArgumentType.NONE.name();
    }
}
