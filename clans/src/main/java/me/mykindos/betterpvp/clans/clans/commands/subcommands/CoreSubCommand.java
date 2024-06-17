package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanCoreTeleportEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class CoreSubCommand extends ClanSubCommand {

    @Inject
    public CoreSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
        this.aliases.add("home");
    }

    @Override
    public String getName() {
        return "core";
    }

    @Override
    public String getDescription() {
        return "Teleport to your clan core";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        clanManager.getClanByPlayer(player).ifPresent(playerClan -> {
            if (!playerClan.getCore().isSet()) {
                UtilMessage.simpleMessage(player, "Clans", "Your clan core has not been set yet. Use <yellow>/clan setcore</yellow> to set it.");
                return;
            }

            UtilServer.callEvent(new ClanCoreTeleportEvent(player, () -> playerClan.getCore().teleport(player, true)));
        });
    }
}
