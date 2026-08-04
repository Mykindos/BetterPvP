package me.mykindos.betterpvp.clans.world.ship.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code /ship list} — every mooring in the world you are standing in, and what is tied up at it.
 */
@Singleton
@SubCommand(ShipCommand.class)
public class ShipListSubCommand extends Command {

    private final ShipService ships;

    @Inject
    public ShipListSubCommand(@NotNull ShipService ships) {
        this.ships = ships;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List the moorings in this world";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final List<Berth> berths = ships.berths(player.getWorld());
        if (berths.isEmpty()) {
            UtilMessage.simpleMessage(player, ShipCommand.PREFIX,
                    Component.text("No moorings in this world."));
            return;
        }

        UtilMessage.simpleMessage(player, ShipCommand.PREFIX, Component.text(berths.size(), NamedTextColor.YELLOW)
                .append(Component.text(" mooring(s) in ", NamedTextColor.GRAY))
                .append(Component.text(player.getWorld().getName(), NamedTextColor.AQUA)));
        for (Berth berth : berths) {
            UtilMessage.simpleMessage(player, ShipCommand.PREFIX, Component.text(berth.getId(), NamedTextColor.WHITE)
                    .append(Component.space())
                    .append(state(player, berth)));
        }
    }

    /**
     * What is at this berth. A disabled berth and a genuinely empty one both hold no vessel, so the service is asked
     * which it is rather than guessing from the missing structure name.
     */
    private @NotNull Component state(@NotNull Player player, @NotNull Berth berth) {
        if (ships.isDisabled(player.getWorld(), berth.getId())) {
            return Component.text("disabled", NamedTextColor.RED);
        }
        if (!berth.isCrewable()) {
            return Component.text("empty", NamedTextColor.GRAY);
        }
        return Component.text(berth.getStructure(), NamedTextColor.GREEN)
                .append(Component.text(" capacity " + berth.getCapacity(), NamedTextColor.GRAY));
    }

}
