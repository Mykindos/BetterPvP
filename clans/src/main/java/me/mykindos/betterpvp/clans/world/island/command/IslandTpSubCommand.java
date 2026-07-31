package me.mykindos.betterpvp.clans.world.island.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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
 * {@code /island tp <id>} — teleports the sender to the instance's world spawn. Accepts any unique case-insensitive
 * prefix of an instance id, not just the full UUID.
 */
@Singleton
@SubCommand(IslandCommand.class)
public class IslandTpSubCommand extends Command {

    private final IslandInstanceManager instanceManager;

    @Inject
    public IslandTpSubCommand(@NotNull IslandInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleport to an island instance's world spawn";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("Usage: /island tp <id>", NamedTextColor.RED));
            return;
        }

        final Optional<IslandInstance> instanceOptional = IslandIdResolver.resolve(player, instanceManager, args[0]);
        if (instanceOptional.isEmpty()) {
            return;
        }

        final IslandInstance instance = instanceOptional.get();
        final World world = Bukkit.getWorld(instance.getWorldName());
        if (world == null) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("Instance ", NamedTextColor.RED)
                    .append(Component.text(instance.getId().toString(), NamedTextColor.YELLOW))
                    .append(Component.text("'s world is not loaded.", NamedTextColor.RED)));
            return;
        }

        player.teleport(world.getSpawnLocation());
        UtilMessage.simpleMessage(player, "Islands", Component.text("Teleported to instance ", NamedTextColor.GREEN)
                .append(Component.text(instance.getId().toString(), NamedTextColor.YELLOW))
                .append(Component.text(".", NamedTextColor.GREEN)));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length != 1) {
            return completions;
        }

        final String arg = args[0].toLowerCase();
        instanceManager.all().forEach(instance -> {
            final String shortId = instance.getShortId();
            if (shortId.startsWith(arg)) {
                completions.add(shortId);
            }
        });

        return completions;
    }

}
