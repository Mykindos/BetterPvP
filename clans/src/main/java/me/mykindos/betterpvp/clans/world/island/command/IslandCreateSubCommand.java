package me.mykindos.betterpvp.clans.world.island.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.island.IslandHostRouter;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import me.mykindos.betterpvp.clans.world.island.IslandTemplate;
import me.mykindos.betterpvp.clans.world.island.IslandTemplateRegistry;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /island create <template>} — allocates a fresh instance of the given template and reports its id and
 * world name back to the admin.
 */
@Singleton
@SubCommand(IslandCommand.class)
public class IslandCreateSubCommand extends Command {

    private final Clans clans;
    private final IslandTemplateRegistry templateRegistry;
    private final IslandInstanceManager instanceManager;
    private final IslandHostRouter router;

    @Inject
    public IslandCreateSubCommand(@NotNull Clans clans, @NotNull IslandTemplateRegistry templateRegistry,
                                   @NotNull IslandInstanceManager instanceManager, @NotNull IslandHostRouter router) {
        this.clans = clans;
        this.templateRegistry = templateRegistry;
        this.instanceManager = instanceManager;
        this.router = router;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Allocate a new island instance from a template";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("Usage: /island create <template>", NamedTextColor.RED));
            return;
        }

        final Optional<IslandTemplate> templateOptional = templateRegistry.get(args[0]);
        if (templateOptional.isEmpty()) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("No island template named ", NamedTextColor.RED)
                    .append(Component.text(args[0], NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.RED)));
            return;
        }

        final IslandTemplate template = templateOptional.get();
        if (!router.isLocal(template)) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("Island template ", NamedTextColor.RED)
                    .append(Component.text(template.getKey(), NamedTextColor.YELLOW))
                    .append(Component.text(" is hosted by '" + router.hostFor(template) + "', not this server.", NamedTextColor.RED)));
            return;
        }

        instanceManager.allocate(template).thenAccept(instance ->
                UtilServer.runTask(clans, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    UtilMessage.simpleMessage(player, "Islands", Component.text("Allocated instance ", NamedTextColor.GREEN)
                            .append(Component.text(instance.getId().toString(), NamedTextColor.YELLOW))
                            .append(Component.text(" at world ", NamedTextColor.GREEN))
                            .append(Component.text(instance.getWorldName(), NamedTextColor.YELLOW))
                            .append(Component.text(".", NamedTextColor.GREEN)));
                })
        ).exceptionally(ex -> {
            UtilServer.runTask(clans, () -> {
                if (player.isOnline()) {
                    UtilMessage.simpleMessage(player, "Islands", Component.text("Failed to allocate island instance: ", NamedTextColor.RED)
                            .append(Component.text(String.valueOf(ex.getMessage()), NamedTextColor.RED)));
                }
            });
            return null;
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length != 1) {
            return completions;
        }

        final String arg = args[0].toLowerCase();
        templateRegistry.all().forEach(template -> {
            if (template.getKey().toLowerCase().startsWith(arg)) {
                completions.add(template.getKey());
            }
        });

        return completions;
    }

}
