package me.mykindos.betterpvp.core.command.commands.admin.zone;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

/**
 * {@code /zone discovery reset <player>} — wipes all of a player's discoveries (primary testing tool: makes every
 * discoverable zone notify again on the next visit).
 */
@Singleton
@SubCommand(ZoneDiscoverySubCommand.class)
public class ZoneDiscoveryResetSubCommand extends AbstractZoneDiscoverySubCommand {

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Wipe all of a player's zone discoveries";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final String[] stripped = stripName(args);
        if (stripped.length < 1) {
            UtilMessage.message(player, "Zones", "<yellow>Usage:</yellow> /zone discovery reset <player>");
            return;
        }

        final String targetName = stripped[0];
        clientManager.search(player).offline(targetName).thenAccept(result -> {
            if (result.isEmpty()) {
                UtilServer.runTask(core, () -> UtilMessage.message(player, "Zones", "<red>Player <yellow>%s</yellow> was not found.", targetName));
                return;
            }
            final Client target = result.get();
            discoveryService.reset(target).thenRun(() -> UtilServer.runTask(core, () ->
                    UtilMessage.message(player, "Zones", "<green>Reset</green> all zone discoveries for <yellow>%s</yellow>.", target.getName())));
        });
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
