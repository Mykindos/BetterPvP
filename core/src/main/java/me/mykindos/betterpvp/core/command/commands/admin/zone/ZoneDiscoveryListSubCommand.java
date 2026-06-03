package me.mykindos.betterpvp.core.command.commands.admin.zone;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.discovery.ZoneDiscoveryRecord;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * {@code /zone discovery list <player>} — prints the zones a player has discovered, with timestamps.
 */
@Singleton
@SubCommand(ZoneDiscoverySubCommand.class)
public class ZoneDiscoveryListSubCommand extends AbstractZoneDiscoverySubCommand {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List the zones a player has discovered";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final String[] stripped = stripName(args);
        if (stripped.length < 1) {
            UtilMessage.message(player, "Zones", "<yellow>Usage:</yellow> /zone discovery list <player>");
            return;
        }

        final String targetName = stripped[0];
        clientManager.search(player).offline(targetName).thenAccept(result -> {
            if (result.isEmpty()) {
                UtilServer.runTask(core, () -> UtilMessage.message(player, "Zones", "<red>Player <yellow>%s</yellow> was not found.", targetName));
                return;
            }
            final Client target = result.get();
            discoveryService.list(target).thenAccept(records ->
                    UtilServer.runTask(core, () -> printDiscoveries(player, target, records)));
        });
    }

    private void printDiscoveries(Player player, Client target, List<ZoneDiscoveryRecord> records) {
        if (records.isEmpty()) {
            UtilMessage.message(player, "Zones", "<yellow>%s</yellow> has not discovered any zones.", target.getName());
            return;
        }
        UtilMessage.message(player, "Zones", "<yellow>%s</yellow> has discovered <yellow>%s</yellow> zone(s):", target.getName(), records.size());
        for (ZoneDiscoveryRecord record : records) {
            UtilMessage.message(player, "Zones", "  <white>%s</white> <dark_gray>(%s) <gray>%s",
                    record.getDisplayName(), record.getZoneKey(), TIMESTAMP_FORMAT.format(record.getDiscoveredAt()));
        }
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
