package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.delayedactions.events.PlayerDelayedTeleportEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class HomeSubCommand extends ClanSubCommand {

    public HomeSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "home";
    }

    @Override
    public String getDescription() {
        return "Teleport to your clan home";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();;
        if (playerClan.getHome() == null) {
            UtilMessage.message(player, "Clans", "Your clan has not set a home");
            return;
        }

        UtilServer.callEvent(new PlayerDelayedTeleportEvent(player, () -> player.teleport(playerClan.getHome())));
    }
}
