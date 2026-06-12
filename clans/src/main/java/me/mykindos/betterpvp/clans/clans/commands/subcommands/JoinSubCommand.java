package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.Zones;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class JoinSubCommand extends ClanSubCommand {

    private final ZoneManager zoneManager;

    @Inject
    public JoinSubCommand(ClanManager clanManager, ClientManager clientManager, ZoneManager zoneManager) {
        super(clanManager, clientManager);
        this.zoneManager = zoneManager;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "clans.command.join.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.join.no-args");
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isPresent()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.join.already-in-clan");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0].replace("_", " "));
        targetClanOptional.ifPresent(targetClan -> {
            Optional<Clan> clanByLocationOptional = clanManager.getClanByLocation(player.getLocation());
            if (clanByLocationOptional.isPresent()) {
                Clan locationClan = clanByLocationOptional.get();
                if (!zoneManager.hasTagAt(player.getLocation(), Zones.SAFE) && locationClan.getId() != targetClan.getId()) {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.join.unsafe-location");
                    return;
                }
            }

            UtilServer.callEvent(new MemberJoinClanEvent(player, targetClan));
        });
    }

    @Override
    public String getArgumentType(int arg) {
        return ClanArgumentType.CLAN.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
