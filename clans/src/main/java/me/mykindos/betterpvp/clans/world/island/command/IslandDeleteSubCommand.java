package me.mykindos.betterpvp.clans.world.island.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
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
import java.util.UUID;

/**
 * {@code /island delete <id> [force]} — releases an island instance, destroying its world. Refuses while occupants
 * remain unless {@code force} is passed. Accepts any unique case-insensitive prefix of an instance id.
 */
@Singleton
@SubCommand(IslandCommand.class)
public class IslandDeleteSubCommand extends Command {

    private final Clans clans;
    private final IslandInstanceManager instanceManager;

    @Inject
    public IslandDeleteSubCommand(@NotNull Clans clans, @NotNull IslandInstanceManager instanceManager) {
        this.clans = clans;
        this.instanceManager = instanceManager;
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Release an island instance";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("Usage: /island delete <id> [force]", NamedTextColor.RED));
            return;
        }

        final Optional<IslandInstance> instanceOptional = IslandIdResolver.resolve(player, instanceManager, args[0]);
        if (instanceOptional.isEmpty()) {
            return;
        }

        final UUID id = instanceOptional.get().getId();
        final boolean force = args.length > 1 && Boolean.parseBoolean(args[1]);

        instanceManager.release(id, force).thenRun(() -> UtilServer.runTask(clans, () -> {
            if (player.isOnline()) {
                UtilMessage.simpleMessage(player, "Islands", Component.text("Released instance ", NamedTextColor.GREEN)
                        .append(Component.text(id.toString(), NamedTextColor.YELLOW))
                        .append(Component.text(".", NamedTextColor.GREEN)));
            }
        }));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            final String arg = args[0].toLowerCase();
            instanceManager.all().forEach(instance -> {
                final String shortId = instance.getShortId();
                if (shortId.startsWith(arg)) {
                    completions.add(shortId);
                }
            });
            return completions;
        }

        if (args.length == 2) {
            if ("force".startsWith(args[1].toLowerCase())) {
                completions.add("force");
            }
            return completions;
        }

        return completions;
    }

}
