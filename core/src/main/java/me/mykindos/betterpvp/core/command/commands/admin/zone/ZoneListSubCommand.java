package me.mykindos.betterpvp.core.command.commands.admin.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code /zone list [namespace]} — lists every registered zone (including provider-sourced ones such as clan
 * territories via {@link ZoneManager#getAllZones()}), grouped by key namespace. Namespaces with more than
 * {@link #FOLD_THRESHOLD} zones collapse to a single count line; pass a namespace to expand just that group.
 */
@Singleton
@SubCommand(ZoneCommand.class)
public class ZoneListSubCommand extends Command {

    /** Above this many zones in a namespace, the group is folded to a count line unless explicitly expanded. */
    private static final int FOLD_THRESHOLD = 10;
    private static final String NAMESPACE = "NAMESPACE";

    private final ZoneManager zoneManager;

    @Inject
    public ZoneListSubCommand(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List all registered zones, grouped by namespace";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final String filter = args.length > 0 ? args[0].toLowerCase() : null;

        final Map<String, List<Zone>> byNamespace = new TreeMap<>();
        for (Zone zone : zoneManager.getAllZones()) {
            byNamespace.computeIfAbsent(zone.getKey().namespace(), ns -> new ArrayList<>()).add(zone);
        }

        if (byNamespace.isEmpty()) {
            UtilMessage.message(player, "Zones", "No zones are registered.");
            return;
        }

        if (filter != null && !byNamespace.containsKey(filter)) {
            UtilMessage.message(player, "Zones", "<red>No zones in namespace <yellow>%s</yellow>.", filter);
            return;
        }

        final int total = byNamespace.values().stream().mapToInt(List::size).sum();
        UtilMessage.message(player, "Zones", "<yellow>%s</yellow> zones across <yellow>%s</yellow> namespace(s)%s",
                total, byNamespace.size(), filter != null ? " <gray>(filtered: " + filter + ")" : "");

        for (Map.Entry<String, List<Zone>> entry : byNamespace.entrySet()) {
            final String namespace = entry.getKey();
            if (filter != null && !namespace.equals(filter)) {
                continue;
            }

            final List<Zone> zones = entry.getValue();
            if (filter == null && zones.size() > FOLD_THRESHOLD) {
                UtilMessage.message(player, "Zones", "<aqua>%s</aqua> <gray>— %s zones <dark_gray>(/zone list %s to expand)",
                        namespace, zones.size(), namespace);
                continue;
            }

            UtilMessage.message(player, "Zones", "<aqua>%s</aqua> <gray>(%s)", namespace, zones.size());
            zones.sort(Comparator.comparing(zone -> zone.getKey().value()));
            for (Zone zone : zones) {
                final String marker = zone.isDiscoverable() ? "<gold>✦</gold> " : "";
                final String tags = zone.getTags().isEmpty() ? "" : " <dark_gray>" + zone.getTags();
                final String world = zone.getWorld() != null ? " <dark_gray>(" + zone.getWorld().getName() + ")" : "";
                UtilMessage.message(player, "Zones", "  %s<white>%s</white> <gray>%s%s%s",
                        marker, zone.getKey().value(), plain(zone.getDisplayName()), tags, world);
            }
        }
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 0) {
            return completions;
        }
        if (NAMESPACE.equals(getArgumentType(args.length))) {
            final String arg = args[args.length - 1].toLowerCase();
            zoneManager.getAllZones().stream()
                    .map(zone -> zone.getKey().namespace())
                    .distinct()
                    .filter(namespace -> namespace.toLowerCase().startsWith(arg))
                    .forEach(completions::add);
        }
        return completions;
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? NAMESPACE : ArgumentType.NONE.name();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
