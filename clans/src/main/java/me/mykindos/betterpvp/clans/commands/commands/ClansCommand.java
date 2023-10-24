package me.mykindos.betterpvp.clans.commands.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.commands.ClansCommandLoader;
import me.mykindos.betterpvp.clans.fields.Fields;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.clans.tips.ClansTipLoader;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class ClansCommand extends Command implements IConsoleCommand {

    @Override
    public String getName() {
        return "clans";
    }

    @Override
    public String getDescription() {
        return "Base clans command";
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

    @Singleton
    @SubCommand(ClansCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Clans clans;

        @Inject
        private ClansCommandLoader commandLoader;

        @Inject
        private ClansListenerLoader listenerLoader;

        @Inject
        private ClansTipLoader clansTipLoader;

        @Inject
        private Fields fields;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the clans plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            clans.reloadConfig();

            commandLoader.reload(clans.getClass().getPackageName());
            listenerLoader.reload(clans.getClass().getPackageName());
            clansTipLoader.reload(clans.getClass().getPackageName());

            fields.reload(clans);
            clans.getInjector().injectMembers(clans.getInjector().getInstance(MapHandler.class));


            UtilMessage.message(sender, "Clans", "Successfully reloaded clans");
        }
    }
}
