package me.mykindos.betterpvp.core.client.commands;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ClientCommand extends Command {

    @WithReflection
    public ClientCommand() {
        subCommands.add(new AdminSubCommand());
    }

    @Override
    public String getName() {
        return "client";
    }

    @Override
    public String getDescription() {
        return "Base client command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Command", "You must specify a sub command");
    }

    private static class AdminSubCommand extends SubCommand {

        @Override
        public String getName() {
            return "admin";
        }

        @Override
        public String getDescription() {
            return "Enable administration mode";
        }

        @Override
        public void execute(Player player, Client client, String[] args) {
            client.setAdministrating(!client.isAdministrating());
            UtilMessage.message(player, "Command", "Client admin: "
                    + (client.isAdministrating() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }
}
