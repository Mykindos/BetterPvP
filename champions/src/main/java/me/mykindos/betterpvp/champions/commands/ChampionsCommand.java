package me.mykindos.betterpvp.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.listeners.ChampionsListenerLoader;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsStatsRepository;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class ChampionsCommand extends Command implements IConsoleCommand {


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
        return Rank.ADMIN;
    }

    @Singleton
    @SubCommand(ChampionsCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Champions champions;

        @Inject
        private ChampionsCommandLoader commandLoader;

        @Inject
        private ChampionsListenerLoader listenerLoader;

        @Inject
        private ChampionsSkillManager skillManager;

        @Inject
        private BuildManager buildManager;

        @Inject
        private TipManager tipManager;

        @Inject
        private BrigadierChampionsCommandLoader brigadierChampionsCommandLoader;

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
            champions.reload();
            champions.getReloadables().forEach(Reloadable::reload);

            brigadierChampionsCommandLoader.reload();
            commandLoader.reload(champions.getClass().getPackageName());
            skillManager.reloadSkills();
            buildManager.reloadBuilds();
            tipManager.reloadTips(champions);

            UtilMessage.message(sender, "Champions", "Successfully reloaded champions");
        }
    }

    @Singleton
    @SubCommand(ChampionsCommand.class)
    private static class ValidateSubCommand extends Command implements IConsoleCommand {

        private final ClientManager clientManager;
        private final ChampionsStatsRepository stats;

        @Inject
        private ValidateSubCommand(ClientManager clientManager, ChampionsStatsRepository stats) {
            this.clientManager = clientManager;
            this.stats = stats;
        }

        @Override
        public String getName() {
            return "validate";
        }

        @Override
        public String getDescription() {
            return "Change validation of players stats";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) return;
            boolean isValid = Boolean.parseBoolean(args[1]);
            clientManager.search().offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isPresent()) {
                    final Client targetClient = targetOptional.get();
                    stats.validate(targetClient, isValid);

                    UtilMessage.simpleMessage(sender, "Champions", "Successfully invalidated <yellow>%s's</yellow> stats", targetClient.getName());
                }

            });
        }
    }

    @Override
    public String getArgumentType(int arg) {
        return switch (arg) {
            case 1 -> ArgumentType.SUBCOMMAND.name();
            case 2 -> ArgumentType.PLAYER.name();
            default -> ArgumentType.BOOLEAN.name();
        };
    }
}
