package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class ClanRecoveryCommand extends ClanSubCommand {

    @Inject
    public ClanRecoveryCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "clanrecovery";
    }

    @Override
    public String getDescription() {
        return "Trigger clan recovery for the current clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        gamerManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> triggered clan recovery for <yellow>%s", player.getName(), clan.getName()), Rank.HELPER);

        clanManager.startInsuranceRollback(clan);

    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
