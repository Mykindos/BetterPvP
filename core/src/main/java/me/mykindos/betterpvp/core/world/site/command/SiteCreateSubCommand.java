package me.mykindos.betterpvp.core.world.site.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.site.Party;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /site create <site> [owner]}: opens an instance without travelling to it.
 */
@Singleton
@SubCommand(SiteCommand.class)
public class SiteCreateSubCommand extends Command {

    private final SiteRegistry registry;
    private final SiteInstances instances;

    @Inject
    public SiteCreateSubCommand(@NotNull SiteRegistry registry, @NotNull SiteInstances instances) {
        this.registry = registry;
        this.instances = instances;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Open an instance of a site";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("Usage: /site create <site> [owner]", NamedTextColor.RED));
            return;
        }

        final Site site = registry.get(args[0]).orElse(null);
        if (site == null) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("No site named ", NamedTextColor.RED)
                    .append(Component.text(args[0], NamedTextColor.YELLOW)));
            return;
        }

        final SiteKey key = args.length > 1 ? site.keyFor(Long.parseLong(args[1])) : site.key();
        instances.locate(key, Party.solo(player.getUniqueId())).whenComplete((instance, ex) -> {
            if (ex != null) {
                UtilMessage.simpleMessage(player, "Sites", Component.text("Could not open one: " + ex.getMessage(), NamedTextColor.RED));
                return;
            }

            // Nobody is going there yet, so the reservation locating it made has to be handed back.
            instances.abandon(instance, Party.solo(player.getUniqueId()));
            UtilMessage.simpleMessage(player, "Sites", Component.text("Opened ", NamedTextColor.GREEN)
                    .append(Component.text(instance.getShortId(), NamedTextColor.YELLOW))
                    .append(Component.text(" at " + instance.getWorldName(), NamedTextColor.GRAY)));
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length != 1) {
            return completions;
        }

        registry.all().stream()
                .map(Site::getId)
                .filter(id -> id.startsWith(args[0].toLowerCase()))
                .forEach(completions::add);
        return completions;
    }
}
