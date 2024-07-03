package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.clans.menus.EnergyMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@SubCommand(ClanCommand.class)
@Singleton
public class EnergyShopSubCommand extends ClanSubCommand {

    public final Clans clans;

    @Inject
    public EnergyShopSubCommand(ClanManager clanManager, ClientManager clientManager, Clans clans) {
        super(clanManager, clientManager);
        this.clans = clans;
    }

    @Override
    public String getName() {
        return "energyshop";
    }

    @Override
    public String getDescription() {
        return "Open your Clans energy shop";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        UtilServer.runTask(clans, () -> {
            Windowed parent = new ClanMenu(player, playerClan, playerClan);
            new EnergyMenu(playerClan, parent).show(player);
        } );
    }
}
