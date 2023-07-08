package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class InfoSubCommand extends ClanSubCommand {

    @Inject
    public InfoSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "View another clans information";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a clan name.");
            return;
        }

        String clanName = String.join(" ", args).trim();
        Optional<Clan> clanOptional = clanManager.getClanByName(clanName);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Could not find a clan with that name.");
            return;
        }

        Clan target = clanOptional.get();


        Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
        ClanRelation clanRelation = clanManager.getRelation(playerClan, target);

        UtilMessage.simpleMessage(player, "Clans", clanRelation.getSecondaryMiniColor() + target.getName() + " Information: ");
        UtilMessage.simpleMessage(player, "Age: <yellow>" + target.getAge());
        UtilMessage.simpleMessage(player, "Territory: <yellow>" + target.getTerritory().size() + "/" + (3 + target.getMembers().size()));
        UtilMessage.simpleMessage(player, "Allies: " + clanManager.getAllianceList(player, target));
        UtilMessage.simpleMessage(player, "Enemies: " + clanManager.getEnemyList(player, target));
        UtilMessage.simpleMessage(player, "Members: " + clanManager.getMembersList(target));
        // UtilMessage.message(player, "TNT Protection: " + clan.getVulnerableString());
        // UtilMessage.message(player, "Cooldown: " + (!clan.isOnCooldown() ? ChatColor.GREEN + "No"
        //         : ChatColor.RED + UtilTime.getTime(clan.getCooldown(), TimeUnit.BEST, 2)));
        UtilMessage.simpleMessage(player, "Energy: <yellow>" + target.getEnergy() + " - (<gold>"
                + target.getEnergyTimeRemaining() + "<yellow>)");
        UtilMessage.simpleMessage(player, "Level: <yellow>%d", target.getLevel());

        if (clanRelation == ClanRelation.ENEMY) {
            UtilMessage.message(player, "Dominance: " + Objects.requireNonNull(playerClan).getDominanceString(target));
        }

        if (client.hasRank(Rank.ADMIN)) {
            UtilMessage.simpleMessage(player, "Points: <yellow>%d", target.getPoints());
        }
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ClanArgumentType.CLAN.name();
        }

        return ArgumentType.NONE.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
