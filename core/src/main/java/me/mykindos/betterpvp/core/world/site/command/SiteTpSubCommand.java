package me.mykindos.betterpvp.core.world.site.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /site tp <id>}: teleports to an instance's world by id prefix.
 */
@Singleton
@SubCommand(SiteCommand.class)
public class SiteTpSubCommand extends Command {

    private final SiteInstances instances;

    @Inject
    public SiteTpSubCommand(@NotNull SiteInstances instances) {
        this.instances = instances;
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleport to a site instance";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("Usage: /site tp <id>", NamedTextColor.RED));
            return;
        }

        final Optional<SiteInstance> found = SiteIds.resolve(player, instances, args[0]);
        if (found.isEmpty()) {
            return;
        }

        final SiteInstance instance = found.get();
        final World world = Bukkit.getWorld(instance.getWorldName());
        if (world == null) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("That instance's world is not loaded.", NamedTextColor.RED));
            return;
        }

        player.teleportAsync(world.getSpawnLocation());
        UtilMessage.simpleMessage(player, "Sites", Component.text("Teleported to ", NamedTextColor.GREEN)
                .append(Component.text(instance.getShortId(), NamedTextColor.YELLOW)));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length != 1) {
            return completions;
        }

        instances.all().stream()
                .map(SiteInstance::getShortId)
                .filter(id -> id.startsWith(args[0].toLowerCase()))
                .forEach(completions::add);
        return completions;
    }
}
