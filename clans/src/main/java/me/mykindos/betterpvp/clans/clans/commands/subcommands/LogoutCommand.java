package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanHomeTeleportEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class LogoutCommand extends ClanSubCommand {

    @Inject
    public LogoutCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "logout";
    }

    @Override
    public String getDescription() {
        return "Logout safely in safe territory.";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();;
        if (playerClan.getHome() == null) {
            UtilMessage.message(player, "Clans", "Your clan has not set a home");
            return;
        }

        UtilServer.callEvent(new ClanHomeTeleportEvent(player, () -> player.teleport(playerClan.getHome())));
    }
}
