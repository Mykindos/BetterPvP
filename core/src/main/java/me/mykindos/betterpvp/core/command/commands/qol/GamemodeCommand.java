package me.mykindos.betterpvp.core.command.commands.qol;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GamemodeCommand extends Command {

    @WithReflection
    public GamemodeCommand() {
        aliases.add("gm");

        subCommands.add(new CreativeSubCommand());
        subCommands.add(new SurvivalSubCommand());
        subCommands.add(new AdventureSubCommand());
        subCommands.add(new SpectatorSubCommand());
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

    private static class CreativeSubCommand extends SubCommand {

        public CreativeSubCommand() {
            aliases.add("c");
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

    private static class SurvivalSubCommand extends SubCommand {

        public SurvivalSubCommand() {
            aliases.add("s");
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

    private static class AdventureSubCommand extends SubCommand {

        public AdventureSubCommand() {
            aliases.add("a");
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

    private static class SpectatorSubCommand extends SubCommand {

        public SpectatorSubCommand() {
            aliases.add("sp");
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
