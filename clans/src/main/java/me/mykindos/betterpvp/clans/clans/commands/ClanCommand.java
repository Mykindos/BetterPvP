package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
public class ClanCommand extends Command {

    private final ClanManager clanManager;
    private final GamerManager gamerManager;

    @WithReflection
    @Inject
    public ClanCommand(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;

        aliases.addAll(List.of("c", "f", "faction"));

    }

    @Override
    public String getName() {
        return "clan";
    }

    @Override
    public String getDescription() {
        return "Basic clan command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        clanManager.getClanByPlayer(player).ifPresentOrElse(clan -> {
                    UtilMessage.message(player, "Clans", clan.getName() + " Information: ");

                    UtilMessage.simpleMessage(player, "Age: <yellow>" + clan.getAge());
                    UtilMessage.simpleMessage(player, "Territory: <yellow>" + clan.getTerritory().size() + "/" + (3 + clan.getMembers().size()));
                    UtilMessage.simpleMessage(player, "Home: " + (clan.getHome() == null ? "<red>Not set" : "<yellow>" + UtilWorld.locationToString(clan.getHome())));
                    UtilMessage.simpleMessage(player, "Allies: " + clanManager.getAllianceList(player, clan));
                    UtilMessage.simpleMessage(player, "Enemies: " + clanManager.getEnemyListDom(player, clan));
                    UtilMessage.simpleMessage(player, "Members: " + clanManager.getMembersList(clan));
                    // UtilMessage.message(player, "TNT Protection: " + clan.getVulnerableString());
                    UtilMessage.simpleMessage(player, "Cooldown: " + (!clan.isNoDominanceCooldownActive() ? "<green>No"
                            : "<red>" + UtilTime.getTime(clan.getNoDominanceCooldown() - System.currentTimeMillis(), UtilTime.TimeUnit.BEST, 1)));
                    UtilMessage.simpleMessage(player, "Energy: <yellow>" + clan.getEnergy() + " - (<gold>" + clan.getEnergyTimeRemaining() + "<yellow>)");

                    UtilMessage.simpleMessage(player, "Points: <yellow>%d", clan.getPoints());
                    UtilMessage.simpleMessage(player, "Level: <yellow>%d", clan.getLevel());

                },
                () -> UtilMessage.message(player, "Clans", "You are not in a clan")
        );

    }
}
