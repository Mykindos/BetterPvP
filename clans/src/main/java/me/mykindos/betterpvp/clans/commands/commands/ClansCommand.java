package me.mykindos.betterpvp.clans.commands.commands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.skills.SkillManager;
import me.mykindos.betterpvp.clans.commands.ClansCommandLoader;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import org.bukkit.entity.Player;

public class ClansCommand extends Command {

    @WithReflection
    public ClansCommand() {
        subCommands.add(new ReloadCommand());
    }

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
    public Rank getRequiredRank() {
        return Rank.OWNER;
    }

    private static class ReloadCommand extends SubCommand {

        @Inject
        private Clans clans;

        @Inject
        private ClansCommandLoader commandLoader;

        @Inject
        private ClansListenerLoader listenerLoader;

        @Inject
        private SkillManager skillManager;

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

            clans.reloadConfig();

            commandLoader.reload();
            listenerLoader.reload();
            skillManager.reloadSkills();

        }
    }
}
