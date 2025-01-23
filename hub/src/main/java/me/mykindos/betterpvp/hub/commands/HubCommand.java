package me.mykindos.betterpvp.hub.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.hub.Hub;
import me.mykindos.betterpvp.hub.feature.npc.HubNPCFactory;
import me.mykindos.betterpvp.hub.listener.HubListenerLoader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class HubCommand extends Command implements IConsoleCommand {

    @Override
    public String getName() {
        return "hub";
    }

    @Override
    public String getDescription() {
        return "Hub base command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
    }

    @Override
    public boolean informInsufficientRank() {
        return true;
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Singleton
    @SubCommand(HubCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Hub hub;

        @Inject
        private HubCommandLoader commandLoader;

        @Inject
        private HubListenerLoader listenerLoader;

        @Inject
        private HubNPCFactory npcFactory;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the hub plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            hub.reload();
            commandLoader.reload(hub.getClass().getPackageName());
            npcFactory.tryLoad(hub);
            UtilMessage.message(sender, "Hub", "Successfully reloaded hub");
        }
    }

}
