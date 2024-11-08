package me.mykindos.betterpvp.progression.profession.mining.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CustomLog
@Singleton
public class CaveCallerCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "cavecaller";
    }

    @Override
    public @NotNull String getDescription() {
        return "Toggle Cave Caller on/off";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        player.sendMessage("Cave caller on");
    }
}
