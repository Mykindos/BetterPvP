package me.mykindos.betterpvp.core.command.commands;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.ISubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GamemodeCommand extends Command {

    @WithReflection
    public GamemodeCommand() {
        aliases.add("gm");

        subCommands.add(new CreativeSubCommand());
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
        if (args != null && args.length > 0) {
            String gamemode = args[0];

            try {
                GameMode mode = GameMode.valueOf(gamemode.toUpperCase());
                player.setGameMode(mode);
            } catch (IllegalArgumentException ex) {
                player.sendMessage("Could not find gamemode: " + gamemode);
            }

        }
    }

    private static class CreativeSubCommand implements ISubCommand {

        @Override
        public String getName() {
            return "c";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            player.setGameMode(GameMode.CREATIVE);
        }
    }
}
