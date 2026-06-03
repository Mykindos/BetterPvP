package me.mykindos.betterpvp.core.command.commands.admin.zone;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.discovery.ZoneDiscoveryService;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shared base for the {@code /zone discovery} leaf subcommands: holds the injected services, resolves player and zone
 * arguments, and tab-completes online players ({@code PLAYER}) and discoverable zone keys ({@code ZONE}).
 */
abstract class AbstractZoneDiscoverySubCommand extends Command {

    private static final String ZONE = "ZONE";

    @Inject
    protected ClientManager clientManager;
    @Inject
    protected ZoneDiscoveryService discoveryService;
    @Inject
    protected ZoneManager zoneManager;
    @Inject
    protected Core core;

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 0) {
            return completions;
        }
        final String arg = args[args.length - 1].toLowerCase();
        switch (getArgumentType(args.length)) {
            case "PLAYER" -> Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(arg))
                    .forEach(completions::add);
            case ZONE -> zoneManager.getAllZones().stream()
                    .filter(Zone::isDiscoverable)
                    .map(zone -> zone.getKey().asString())
                    .filter(key -> key.toLowerCase().startsWith(arg))
                    .forEach(completions::add);
        }
        return completions;
    }

    /**
     * Removes a leading token equal to this subcommand's name. Two-level-deep subcommands (under {@code /zone
     * discovery}) receive their own name as {@code args[0]} from the command dispatcher, so this normalizes the args.
     */
    protected String[] stripName(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase(getName())) {
            return Arrays.copyOfRange(args, 1, args.length);
        }
        return args;
    }

    protected @Nullable Zone resolveZone(String keyArg) {
        try {
            return zoneManager.getZone(Key.key(keyArg)).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    protected String zoneArgType() {
        return ZONE;
    }
}
