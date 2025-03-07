package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class LegacyPunishmentCommand extends Command {


    public LegacyPunishmentCommand() {
        aliases.add("lp");
    }

    @Override
    public String getName() {
        return "legacypunish";
    }

    @Override
    public String getDescription() {
        return "Base command for the legacy punishing system";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        //base command
    }
}
