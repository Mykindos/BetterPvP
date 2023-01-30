package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class SetDominanceSubCommand extends ClanSubCommand {

    @Inject
    public SetDominanceSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "setdominance";
    }

    @Override
    public String getDescription() {
        return "Force set the dominance on an enemy clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        int dominance = Integer.parseInt(args[1]);
        if(dominance > 99) {
            UtilMessage.message(player, "Clans", "Dominance must be between 0-99");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Could not find a clan with that name");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        Clan targetClan = targetClanOptional.get();

        ClanEnemy playerClanEnemy = playerClan.getEnemy(targetClan);
        if(playerClanEnemy == null) {
            UtilMessage.message(player, "Clans", "You must be enemies with the target clan to use this command.");
            return;
        }

        ClanEnemy targetClanEnemy = targetClan.getEnemy(playerClan);
        if(targetClanEnemy == null) {
            UtilMessage.message(player, "Clans", "Something went severely wrong. Contact a developer");
            return;
        }

        playerClanEnemy.setDominance(dominance);
        targetClanEnemy.setDominance(0);
        clanManager.getRepository().updateDominance(playerClan, playerClanEnemy);
        clanManager.getRepository().updateDominance(targetClan, targetClanEnemy);

        playerClan.messageClan("<gray>Your dominance against <red>" + targetClan.getName()
                        + " <gray>has been set to <green>" + dominance + "%", null, true);
        targetClan.messageClan("<gray>Your dominance against <red>" + targetClan.getName()
                + " <gray>has been set to <red>-" + dominance + "%", null, true);
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ClanArgumentType.CLAN.name() : ArgumentType.NONE.name();
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
