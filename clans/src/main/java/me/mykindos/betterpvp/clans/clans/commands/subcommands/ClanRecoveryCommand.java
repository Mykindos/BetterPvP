package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

@SubCommand
public class ClanRecoveryCommand extends ClanSubCommand {

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

        List<Insurance> insuranceList = clan.getInsurance();
        insuranceList.sort(Collections.reverseOrder());
        clanManager.getInsuranceQueue().addAll(insuranceList);
        clanManager.getRepository().deleteInsuranceForClan(clan);
        clan.getInsurance().clear();

    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
