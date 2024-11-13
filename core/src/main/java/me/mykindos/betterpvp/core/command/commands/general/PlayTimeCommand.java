package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        String timePlayed = UtilTime.humanReadableFormat(Duration.ofMillis((Long) client.getProperty(ClientProperty.TIME_PLAYED).orElse(0L)));
        UtilMessage.simpleMessage(player, "Command", "You have played for <white>" + timePlayed + "</white>.");
    }
}
