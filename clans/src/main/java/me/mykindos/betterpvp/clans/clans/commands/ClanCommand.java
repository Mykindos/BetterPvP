package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        Optional<Gamer> gamerOptional = gamerManager.getGamerByName(args[0]);
        if (gamerOptional.isPresent()) {
            clanManager.getClanByPlayer(UUID.fromString(gamerOptional.get().getUuid())).ifPresent(clan -> openClanMenu(player, playerClan, clan));
        } else {
            UtilMessage.message(player, "Clans", "Cannot find the specified clan or player");
        }
    }

    private void openClanMenu(Player player, Clan playerClan, Clan clan) {
        Menu clanMenu = new ClanMenu(player, playerClan, clan);
        MenuManager.openMenu(player, clanMenu);
    }
}
