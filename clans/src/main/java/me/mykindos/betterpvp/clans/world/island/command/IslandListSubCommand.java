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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * {@code /island list} — lists every live instance: id, template, world, state and occupant count.
 */
@Singleton
@SubCommand(IslandCommand.class)
public class IslandListSubCommand extends Command {

    private final IslandInstanceManager instanceManager;

    @Inject
    public IslandListSubCommand(@NotNull IslandInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List every live island instance";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Collection<IslandInstance> instances = instanceManager.all();
        if (instances.isEmpty()) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("No island instances are live."));
            return;
        }

        UtilMessage.simpleMessage(player, "Islands", Component.text(instances.size(), NamedTextColor.YELLOW)
                .append(Component.text(" live instance(s):")));
        for (IslandInstance instance : instances) {
            UtilMessage.simpleMessage(player, "Islands", Component.text(instance.getId().toString(), NamedTextColor.WHITE)
                    .append(Component.text(" (" + instance.getTemplate().getKey() + ")", NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(Component.text(instance.getWorldName(), NamedTextColor.AQUA))
                    .append(Component.space())
                    .append(Component.text(instance.getState().toString(), NamedTextColor.YELLOW))
                    .append(Component.text(" occupants: " + instance.getOccupants().size(), NamedTextColor.GRAY)));
        }
    }

}
