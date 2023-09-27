package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class AdminCommand extends ClanSubCommand {

    @WithReflection
    @Inject
    public AdminCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);

        aliases.addAll(List.of("mimic", "admin"));

    }

    @Override
    public String getName() {
        return "x";
    }

    @Override
    public String getDescription() {
        return "Basic clanadmin";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            client.setMimicClan(null);
            UtilMessage.message(player, "Clan Admin", "Reset mimicked clan");
        }

        if (args.length != 1) return;

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Could not find a clan with that name");
            return;
        }

        Clan targetClan = targetClanOptional.get();

        client.setMimicClan(targetClan.getId());
        UtilMessage.message(player, "Clan Admin", "Now mimicking Clan <yellow>" + targetClan.getName());

    }

    public boolean requiresServerAdmin() {
        return true;
    }

    public boolean canExecuteWithoutClan() {
        return true;
    }
}
