package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class PunishCommand extends Command {
    @Override
    public String getName() {
        return "punish";
    }

    @Override
    public String getDescription() {
        return "Base command for punishing system";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        // Base command
    }


}
