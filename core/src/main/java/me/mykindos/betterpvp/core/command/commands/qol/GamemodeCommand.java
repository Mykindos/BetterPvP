package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

@Singleton
public class GamemodeCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public GamemodeCommand(final ClientManager clientManager) {
        this.clientManager = clientManager;

        aliases.add("gm");
    }

    @Override
    public String getName() {
        return "gamemode";
    }

    @Override
    public String getDescription() {
        return "Quickly change gamemode";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Command", "Please specify a valid gamemode");
    }

    private void handleGameMode(Player sender, String[] args, GameMode gameMode, String type) {
        if (args.length == 0) {
            sender.setGameMode(gameMode);

            UtilMessage.simpleMessage(sender, "Gamemode", "Your game mode has been updated to <green>%s</green>.", type);

            this.clientManager.sendMessageToRank("Gamemode", UtilMessage.deserialize("<yellow>%s</yellow> updated their game mode to <green>%s</green>.".formatted(sender.getName(), type)), Rank.ADMIN, Collections.singletonList(sender.getUniqueId()));
            return;
        }

        if (args.length == 1) {
            Player targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer != null) {
                if (sender.equals(targetPlayer)) {
                    handleGameMode(sender, new String[0], gameMode, type);
                    return;
                }

                targetPlayer.setGameMode(gameMode);

                UtilMessage.simpleMessage(sender, "Gamemode", "You updated the game mode for <yellow>%s</yellow> to <green>%s</green>.", targetPlayer.getName(), type);

                UtilMessage.simpleMessage(targetPlayer, "Gamemode", "<yellow>%s</yellow> updated your game mode to <green>%s</green>.", sender.getName(), type);

                this.clientManager.sendMessageToRank("Gamemode", UtilMessage.deserialize("<yellow>%s</yellow> updated the game mode for <yellow>%s</yellow> to <green>%s</green>.".formatted(sender.getName(), targetPlayer.getName(), type)), Rank.ADMIN, List.of(sender.getUniqueId(), targetPlayer.getUniqueId()));
            }
        }
    }

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class SurvivalSubCommand extends Command {

        private final GamemodeCommand gamemodeCommand;

        @Inject
        public SurvivalSubCommand(final GamemodeCommand gamemodeCommand) {
            this.gamemodeCommand = gamemodeCommand;

            aliases.add("s");
            aliases.add("0");
        }

        @Override
        public String getName() {
            return "survival";
        }

        @Override
        public String getDescription() {
            return "Swap to survival mode";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            this.gamemodeCommand.handleGameMode(player, args, GameMode.SURVIVAL, "Survival");
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class CreativeSubCommand extends Command {

        private final GamemodeCommand gamemodeCommand;

        @Inject
        public CreativeSubCommand(final GamemodeCommand gamemodeCommand) {
            this.gamemodeCommand = gamemodeCommand;

            aliases.add("c");
            aliases.add("1");
        }

        @Override
        public String getName() {
            return "creative";
        }

        @Override
        public String getDescription() {
            return "Swap to creative mode";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            this.gamemodeCommand.handleGameMode(player, args, GameMode.CREATIVE, "Creative");
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class AdventureSubCommand extends Command {

        private final GamemodeCommand gamemodeCommand;

        @Inject
        public AdventureSubCommand(final GamemodeCommand gamemodeCommand) {
            this.gamemodeCommand = gamemodeCommand;

            aliases.add("a");
            aliases.add("2");
        }

        @Override
        public String getName() {
            return "adventure";
        }

        @Override
        public String getDescription() {
            return "Swap to adventure mode";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            this.gamemodeCommand.handleGameMode(player, args, GameMode.ADVENTURE, "Adventure");
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class SpectatorSubCommand extends Command {

        private final GamemodeCommand gamemodeCommand;

        @Inject
        public SpectatorSubCommand(final GamemodeCommand gamemodeCommand) {
            this.gamemodeCommand = gamemodeCommand;

            aliases.add("sp");
            aliases.add("3");
        }

        @Override
        public String getName() {
            return "spectator";
        }

        @Override
        public String getDescription() {
            return "Swap to spectator mode";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            this.gamemodeCommand.handleGameMode(player, args, GameMode.SPECTATOR, "Spectator");
        }

        @Override
        public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
        }
    }
}
