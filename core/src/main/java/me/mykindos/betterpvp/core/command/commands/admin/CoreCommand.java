package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CoreCommand extends Command implements IConsoleCommand {

    @WithReflection
    public CoreCommand() {
        subCommands.add(new ReloadCommand());
    }

    @Override
    public String getName() {
        return "core";
    }

    @Override
    public String getDescription() {
        return "Base core command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

    }

    @Override
    public void execute(CommandSender sender, String[] args) {

    }

    @Override
    public Rank getRequiredRank() {
        return Rank.OWNER;
    }

    @SubCommand
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Core core;

        @Inject
        private CoreCommandLoader commandLoader;

        @Inject
        private CoreListenerLoader listenerLoader;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the core plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            core.reloadConfig();
            commandLoader.reload(core.getClass().getPackageName());
            listenerLoader.reload(core.getClass().getPackageName());

            UtilMessage.message(sender, "Core", "Successfully reloaded core");
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.OWNER;
        }


    }
}
