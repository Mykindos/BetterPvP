package me.mykindos.betterpvp.clans.world.camp.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.CrewMenus;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import org.bukkit.entity.Player;

/** {@code /clan crews} shows every job running in the camp you stand in, and who is working on it. */
@Singleton
@SubCommand(ClanCommand.class)
public class CampCrewsSubCommand extends ClanSubCommand {

    private final CrewMenus menus;

    @Inject
    public CampCrewsSubCommand(ClanManager clanManager, ClientManager clientManager, CrewMenus menus) {
        super(clanManager, clientManager);
        this.menus = menus;
    }

    @Override
    public String getName() {
        return "crews";
    }

    @Override
    public String getDescription() {
        return "clans.command.crews.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        menus.openJobs(player);
    }
}
