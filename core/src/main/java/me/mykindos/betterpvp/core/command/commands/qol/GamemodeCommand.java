package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@Singleton
public class GamemodeCommand extends Command {

    @WithReflection
    public GamemodeCommand() {
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

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class SurvivalSubCommand extends Command {

        public SurvivalSubCommand() {
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
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class CreativeSubCommand extends Command {

        public CreativeSubCommand() {
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
            player.setGameMode(GameMode.CREATIVE);
        }
    }

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class AdventureSubCommand extends Command {

        public AdventureSubCommand() {
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
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    @Singleton
    @SubCommand(GamemodeCommand.class)
    private static class SpectatorSubCommand extends Command {

        public SpectatorSubCommand() {
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
            player.setGameMode(GameMode.SPECTATOR);
        }

    }

}
