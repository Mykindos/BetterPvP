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
 * {@code /ship enable <berth|*>} — moors a disabled vessel again, cancelling any timer that would have done it.
 */
@Singleton
@SubCommand(ShipCommand.class)
public class ShipEnableSubCommand extends Command {

    private final ShipService ships;

    @Inject
    public ShipEnableSubCommand(@NotNull ShipService ships) {
        this.ships = ships;
    }

    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public String getDescription() {
        return "Moor a disabled ship again";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, ShipCommand.PREFIX,
                    Component.text("Usage: /ship enable <berth|*>", NamedTextColor.RED));
            return;
        }

        final List<String> berthIds = BerthArgument.resolve(player, ships, args[0]);
        if (berthIds.isEmpty()) {
            return;
        }

        ships.enable(player.getWorld(), berthIds);
        UtilMessage.simpleMessage(player, ShipCommand.PREFIX, Component.text("Moored ", NamedTextColor.GREEN)
                .append(Component.text(berthIds.size() + " berth(s)", NamedTextColor.YELLOW))
                .append(Component.text(" again.", NamedTextColor.GREEN)));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }
        return BerthArgument.complete(player, ships, args[0]);
    }

}
