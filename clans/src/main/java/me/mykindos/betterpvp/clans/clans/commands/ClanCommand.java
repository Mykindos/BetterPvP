package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Singleton
public class ClanCommand extends Command {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @WithReflection
    @Inject
    public ClanCommand(ClanManager clanManager, ClientManager clientManager) {
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

        // If the clan was not found by name, try to get the clan by player name
        final Collection<Client> matches = clientManager.search(player).inform(false).advancedOnline(args[0]);
        if (matches.size() == 1) {
            final Client found = matches.iterator().next();
            final Optional<Clan> foundClan = clanManager.getClanByPlayer(found.getUniqueId());
            foundClan.ifPresentOrElse(clan -> openClanMenu(player, playerClan, clan), () -> {
                UtilMessage.message(player, "Clans", "That player is not in a clan.");
            });
        } else {
            UtilMessage.message(player, "Clans", "Cannot find the specified clan or player.");
        }
    }

    private void openClanMenu(Player player, Clan playerClan, Clan clan) {
        new ClanMenu(player, playerClan, clan).show(player);
    }

}
