package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class JoinSubCommand extends ClanSubCommand {

    public JoinSubCommand(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Join a clan that you have been invited to";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a clan to join");
        }

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if(clanOptional.isPresent()){
            UtilMessage.message(player, "Clans", "You are already in a clan");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getObject(args[0]);
        targetClanOptional.ifPresent(targetClan -> {
            UtilServer.callEvent(new MemberJoinClanEvent(player, targetClan));
        });
    }
}
