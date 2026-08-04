package me.mykindos.betterpvp.clans.world.ship.command;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the {@code <berth|*>} argument the disable and enable subcommands share, resolved against the world the sender
 * is standing in. {@code *} means every mooring here, which is the "show me the dock as it was built" case.
 */
@UtilityClass
class BerthArgument {

    static final String ALL = "*";

    /** Berth ids named by {@code arg}, or an empty list after telling the player why nothing matched. */
    static @NotNull List<String> resolve(@NotNull Player player, @NotNull ShipService ships, @NotNull String arg) {
        final List<Berth> berths = ships.berths(player.getWorld());
        if (berths.isEmpty()) {
            UtilMessage.simpleMessage(player, ShipCommand.PREFIX,
                    Component.text("No moorings in this world.", NamedTextColor.RED));
            return List.of();
        }

        if (ALL.equals(arg)) {
            return berths.stream().map(Berth::getId).toList();
        }

        final List<String> matched = berths.stream()
                .map(Berth::getId)
                .filter(id -> id.equalsIgnoreCase(arg))
                .toList();
        if (matched.isEmpty()) {
            UtilMessage.simpleMessage(player, ShipCommand.PREFIX, Component.text("No mooring called ", NamedTextColor.RED)
                    .append(Component.text(arg, NamedTextColor.YELLOW))
                    .append(Component.text(" in this world.", NamedTextColor.RED)));
        }
        return matched;
    }

    static @NotNull List<String> complete(@NotNull Player player, @NotNull ShipService ships, @NotNull String arg) {
        final String lower = arg.toLowerCase(Locale.ROOT);
        final List<String> completions = new ArrayList<>();
        if (ALL.startsWith(lower)) {
            completions.add(ALL);
        }
        ships.berths(player.getWorld()).stream()
                .map(Berth::getId)
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(lower))
                .forEach(completions::add);
        return completions;
    }
}
