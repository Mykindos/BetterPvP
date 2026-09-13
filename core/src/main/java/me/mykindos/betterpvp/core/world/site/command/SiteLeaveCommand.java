package me.mykindos.betterpvp.core.world.site.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.world.site.SiteExitService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /leavesite}: returns a player to where they set out from.
 */
@Singleton
public class SiteLeaveCommand extends Command {

    private final SiteExitService exitService;

    @Inject
    public SiteLeaveCommand(@NotNull SiteExitService exitService) {
        this.exitService = exitService;
    }

    @Override
    public String getName() {
        return "leavesite";
    }

    @Override
    public String getDescription() {
        return "Leave where you are and return to where you set out from";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        exitService.leave(player);
    }
}
