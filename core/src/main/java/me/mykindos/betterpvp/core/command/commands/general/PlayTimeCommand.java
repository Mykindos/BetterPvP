package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

@Singleton
public class PlayTimeCommand extends Command {

    @Inject
    public PlayTimeCommand() {
        aliases.add("timeplayed");
    }

    @Override
    public String getName() {
        return "playtime";
    }

    @Override
    public String getDescription() {
        return "Check your time played";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        String totalTimePlayed = UtilTime.humanReadableFormat(Duration.ofMillis((Long) client.getProperty(ClientProperty.TIME_PLAYED).orElse(0L)));
        UtilMessage.simpleMessage(player, "Command", "You have played for <white>" + totalTimePlayed + "</white>.");
        if(Bukkit.getPluginManager().getPlugin("Clans") != null) {
            String seasonTimePlayed = UtilTime.humanReadableFormat(Duration.ofMillis((Long) client.getGamer().getProperty(GamerProperty.TIME_PLAYED).orElse(0L)));
            UtilMessage.simpleMessage(player, "Command", "You have played for <white>" + seasonTimePlayed + "</white> this season.");
        }
    }
}
