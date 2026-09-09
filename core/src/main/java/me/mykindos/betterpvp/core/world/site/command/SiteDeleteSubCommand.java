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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /site delete <id> [force]}: destroys an instance and its world.
 */
@Singleton
@SubCommand(SiteCommand.class)
public class SiteDeleteSubCommand extends Command {

    private final SiteInstances instances;

    @Inject
    public SiteDeleteSubCommand(@NotNull SiteInstances instances) {
        this.instances = instances;
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Destroy a site instance and its world";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("Usage: /site delete <id> [force]", NamedTextColor.RED));
            return;
        }

        final Optional<SiteInstance> found = SiteIds.resolve(player, instances, args[0]);
        if (found.isEmpty()) {
            return;
        }

        final SiteInstance instance = found.get();
        final boolean force = args.length > 1 && args[1].equalsIgnoreCase("force");
        if (!force && !instance.isEmpty()) {
            UtilMessage.simpleMessage(player, "Sites", Component.text("That instance still has ", NamedTextColor.RED)
                    .append(Component.text(instance.getOccupants().size() + " occupant(s)", NamedTextColor.YELLOW))
                    .append(Component.text(". Add 'force' to destroy it anyway.", NamedTextColor.RED)));
            return;
        }

        instances.release(instance.getId(), force).thenRun(() ->
                UtilMessage.simpleMessage(player, "Sites", Component.text("Released ", NamedTextColor.GREEN)
                        .append(Component.text(instance.getShortId(), NamedTextColor.YELLOW))));
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
