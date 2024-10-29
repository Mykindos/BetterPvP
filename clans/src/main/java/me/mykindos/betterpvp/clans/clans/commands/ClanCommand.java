package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
public class ClanCommand extends Command {

    private final Clans clans;
    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @WithReflection
    @Inject
    public ClanCommand(Clans clans, ClanManager clanManager, ClientManager clientManager) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.clientManager = clientManager;

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

        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        Clan playerClan = playerClanOptional.orElse(null);

        // If no arguments, show the clan of the current player
        if (args.length == 0) {
            if (playerClan != null) {
                openClanMenu(player, playerClan, playerClan);
            } else {
                UtilMessage.message(player, "Clans", "You are not in a clan");
            }

            return;
        }


        // If there's an argument, first try to get the clan by name
        Optional<Clan> clanByName = clanManager.getClanByName(args[0]);
        if (clanByName.isPresent()) {
            openClanMenu(player, playerClan, clanByName.get());
            return;
        }

        clientManager.search(player).inform(false).advancedOffline(args[0], found -> {
            if(found.size() == 1) {
                final Client targetClient = found.iterator().next();
                final Optional<Clan> foundClan = clanManager.getClanByPlayer(targetClient.getUniqueId());
                foundClan.ifPresentOrElse(clan -> openClanMenu(player, playerClan, clan), () -> {
                    UtilMessage.message(player, "Clans", "That player is not in a clan.");
                });
            } else {
                UtilMessage.message(player, "Clans", "Cannot find the specified clan or player.");
            }
        });


    }

    private void openClanMenu(Player player, Clan playerClan, Clan clan) {
        // We swap back to main thread, because the menu is not thread-safe and can lead to players having a bugged inventory (dupe items)
        UtilServer.runTask(clans, () -> new ClanMenu(player, playerClan, clan).show(player));
    }

}
