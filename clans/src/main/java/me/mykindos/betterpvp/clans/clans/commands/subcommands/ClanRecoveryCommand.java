package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
