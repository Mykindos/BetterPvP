package me.mykindos.betterpvp.clans.clans.transport.commands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.transport.ClanTravelHubMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

public class TransportCommand extends Command {

    private final ClanManager clanManager;

    @Inject
    public TransportCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public String getName() {
        return "transport";
    }

    @Override
    public String getDescription() {
        return "Opens the travel hub UI";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new ClanTravelHubMenu(player, clanManager).show(player);
    }
}
