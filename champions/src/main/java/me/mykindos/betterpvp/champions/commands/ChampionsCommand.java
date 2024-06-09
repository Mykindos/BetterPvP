package me.mykindos.betterpvp.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.listeners.ChampionsListenerLoader;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsStatsRepository;
import me.mykindos.betterpvp.champions.weapons.ChampionsWeaponManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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
        return Rank.OWNER;
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
        private ItemHandler itemHandler;

        @Inject
        private BuildManager buildManager;

        @Inject
        private TipManager tipManager;

        @Inject
        private ChampionsWeaponManager championsWeaponManager;

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

            commandLoader.reload(champions.getClass().getPackageName());
            skillManager.reloadSkills();
            buildManager.reloadBuilds();
            itemHandler.loadItemData("champions");
            tipManager.reloadTips(champions);
            championsWeaponManager.reload();

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
            clientManager.search().advancedOffline(args[0], match -> {
                if (match.size() == 1) {
                    final Client targetClient = match.iterator().next();
                    stats.validate(targetClient, isValid);

                    UtilMessage.simpleMessage(sender, "Champions", "Successfully invalidated <yellow>%s's</yellow> stats", targetClient.getName());
                }

            });
        }
    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
