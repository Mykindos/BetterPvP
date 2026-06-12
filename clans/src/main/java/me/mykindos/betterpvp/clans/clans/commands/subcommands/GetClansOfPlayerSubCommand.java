package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.menu.ClansOfPlayerMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CustomLog
@Singleton
@SubCommand(ClanCommand.class)
public class GetClansOfPlayerSubCommand extends ClanSubCommand {

    @Inject
    public GetClansOfPlayerSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "playerclans";
    }

    @Override
    public String getDescription() {
        return "clans.command.get-clans-of-player.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <player>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length < 1) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.player-clans.usage", Component.text(getUsage()));
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isEmpty()) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.player-clans.not-found", Component.text(args[0], NamedTextColor.GREEN));
                return;
            }
            Client targetClient = clientOptional.get();
            ClansOfPlayerMenu clansOfPlayerMenu = new ClansOfPlayerMenu(targetClient, clanManager, clientManager, null);
            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                clansOfPlayerMenu.show(player);
            });
        });

    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
