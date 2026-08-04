package me.mykindos.betterpvp.clans.world.ship.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code /ship disable <berth|*> [minutes]} — un-moors a vessel and hands its blocks back, so the dock underneath shows
 * as the builder made it. Omit the minutes to leave it off until {@code /ship enable}.
 */
@Singleton
@SubCommand(ShipCommand.class)
public class ShipDisableSubCommand extends Command {

    private final ShipService ships;

    @Inject
    public ShipDisableSubCommand(@NotNull ShipService ships) {
        this.ships = ships;
    }

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public String getDescription() {
        return "Un-moor a ship and restore the blocks it was pasted over";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, ShipCommand.PREFIX,
                    Component.text("Usage: /ship disable <berth|*> [minutes]", NamedTextColor.RED));
            return;
        }

        final List<String> berthIds = BerthArgument.resolve(player, ships, args[0]);
        if (berthIds.isEmpty()) {
            return;
        }

        long minutes = 0;
        if (args.length > 1) {
            try {
                minutes = Long.parseLong(args[1]);
            } catch (NumberFormatException exception) {
                UtilMessage.simpleMessage(player, ShipCommand.PREFIX, Component.text("'", NamedTextColor.RED)
                        .append(Component.text(args[1], NamedTextColor.YELLOW))
                        .append(Component.text("' is not a number of minutes.", NamedTextColor.RED)));
                return;
            }
            if (minutes < 1) {
                UtilMessage.simpleMessage(player, ShipCommand.PREFIX,
                        Component.text("Give at least one minute, or no duration at all.", NamedTextColor.RED));
                return;
            }
        }

        ships.disable(player.getWorld(), berthIds, minutes * 60_000L);

        final Component until = minutes > 0
                ? Component.text(" for " + minutes + " minute(s)", NamedTextColor.GRAY)
                : Component.text(" until re-enabled", NamedTextColor.GRAY);
        UtilMessage.simpleMessage(player, ShipCommand.PREFIX, Component.text("Un-moored ", NamedTextColor.GREEN)
                .append(Component.text(berthIds.size() + " ship(s)", NamedTextColor.YELLOW))
                .append(until)
                .append(Component.text(".", NamedTextColor.GREEN)));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }
        return BerthArgument.complete(player, ships, args[0]);
    }

}
