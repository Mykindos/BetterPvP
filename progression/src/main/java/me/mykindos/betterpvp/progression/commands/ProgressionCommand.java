package me.mykindos.betterpvp.progression.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.ProgressionsManager;
import me.mykindos.betterpvp.progression.commands.loader.ProgressionCommandLoader;
import me.mykindos.betterpvp.progression.listener.ProgressionListenerLoader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class ProgressionCommand extends Command implements IConsoleCommand {

    public ProgressionCommand() {
        aliases.add("progression");
    }

    @Override
    public String getName() {
        return "progression";
    }

    @Override
    public String getDescription() {
        return "Progression base command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {}

    @Override
    public void execute(CommandSender sender, String[] args) {}

    @Override
    public boolean informInsufficientRank() {
        return true;
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Singleton
    @SubCommand(ProgressionCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Progression progression;

        @Inject
        private ProgressionCommandLoader commandLoader;

        @Inject
        private ProgressionListenerLoader listenerLoader;

        @Inject
        private ProgressionsManager progressionsManager;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the progression plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            progression.reloadConfig();

            commandLoader.reload(progression.getClass().getPackageName());
            listenerLoader.reload(progression.getClass().getPackageName());
            progressionsManager.loadTrees();

            UtilMessage.message(sender, "Progression", "Successfully reloaded progression");
        }
    }

}
