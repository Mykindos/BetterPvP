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
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class SetExpSubCommand extends ClanSubCommand {

    @Inject
    public SetExpSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "setexp";
    }

    @Override
    public String getDescription() {
        return "Force set a clan's experience";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <experience>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 1) {
            UtilMessage.message(player, "Clans", "Usage: <alt2>/clan setexp <experience>");
            return;
        }

        long experience;
        try {
            experience = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "Clans", "Invalid number!");
            return;
        }

        if (experience < 0) {
            UtilMessage.message(player, "Clans", "Experience cannot be negative!");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();

        long prevExperience = playerClan.getExperience();
        long prevLevel = playerClan.getLevel();
        playerClan.setExperience(experience);
        long newExperience = playerClan.getExperience();
        long newLevel = playerClan.getLevel();

        UtilMessage.message(player,
                "Clans",
                "Set clan <alt>%s</alt>'s experience from <alt2>%,d (level %,d)</alt2> to <alt2>%,d (level %,d)</alt2>.",
                playerClan.getName(),
                prevExperience,
                prevLevel,
                newExperience,
                newLevel);


        final Component alert = UtilMessage.deserialize("%s set clan <alt>%s</alt>'s experience from <alt2>%,d (level %,d)</alt2> to <alt2>%,d (level %,d)</alt2>.",
                client.getName(),
                playerClan.getName(),
                prevExperience,
                prevLevel,
                newExperience,
                newLevel);
        gamerManager.sendMessageToRank("Clans", alert, Rank.HELPER);
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
