package me.mykindos.betterpvp.champions.commands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.champions.listeners.ChampionsListenerLoader;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChampionsCommand extends Command implements IConsoleCommand {

    @WithReflection
    public ChampionsCommand() {
        subCommands.add(new ReloadCommand());
    }

    @Override
    public String getName() {
        return "champions";
    }

    @Override
    public String getDescription() {
        return "Base champions command";
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


    private static class ReloadCommand extends SubCommand implements IConsoleCommand {

        @Inject
        private Champions champions;

        @Inject
        private ChampionsCommandLoader commandLoader;

        @Inject
        private ChampionsListenerLoader listenerLoader;

        @Inject
        private SkillManager skillManager;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the champions plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            champions.reloadConfig();

            commandLoader.reload();
            listenerLoader.reload();
            skillManager.reloadSkills();

            UtilMessage.message(sender, "champions", "Successfully reloaded champions");
        }
    }
}
