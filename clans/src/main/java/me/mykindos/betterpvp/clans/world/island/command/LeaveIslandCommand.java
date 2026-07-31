package me.mykindos.betterpvp.clans.world.island.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.island.IslandExitService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /leaveisland} — lets any player on a discovery island return to where they departed from.
 */
@Singleton
public class LeaveIslandCommand extends Command {

    private final IslandExitService exitService;

    @Inject
    public LeaveIslandCommand(@NotNull IslandExitService exitService) {
        this.exitService = exitService;
    }

    @Override
    public String getName() {
        return "leaveisland";
    }

    @Override
    public String getDescription() {
        return "Leave your current discovery island";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        exitService.leave(player);
    }

}
