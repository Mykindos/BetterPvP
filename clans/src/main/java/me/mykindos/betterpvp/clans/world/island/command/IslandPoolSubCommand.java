package me.mykindos.betterpvp.clans.world.island.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.island.IslandWarmPool;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * {@code /island pool} — reports how many {@code POOLED} instances are currently on hand per template.
 */
@Singleton
@SubCommand(IslandCommand.class)
public class IslandPoolSubCommand extends Command {

    private final IslandWarmPool warmPool;

    @Inject
    public IslandPoolSubCommand(@NotNull IslandWarmPool warmPool) {
        this.warmPool = warmPool;
    }

    @Override
    public String getName() {
        return "pool";
    }

    @Override
    public String getDescription() {
        return "Report warm pool depth per island template";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Map<String, Integer> depths = warmPool.depths();
        if (depths.isEmpty()) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("No island templates are registered."));
            return;
        }

        UtilMessage.simpleMessage(player, "Islands", Component.text("Warm pool depth:"));
        depths.forEach((templateKey, depth) ->
                UtilMessage.simpleMessage(player, "Islands", Component.text(templateKey, NamedTextColor.WHITE)
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(Component.text(depth, NamedTextColor.YELLOW))));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

}
