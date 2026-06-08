package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class DailyCommand extends Command {

    @Inject
    @Config(path = "daily.coinsAmount", defaultValue = "5000")
    private int coinsAmount;

    public static String DAILY = "Daily";

    private final CooldownManager cooldownManager;

    @Inject
    public DailyCommand(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    @Override
    public String getName() {
        return "daily";
    }

    @Override
    public String getDescription() {
        return "core.command.daily.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(cooldownManager.use(player, DAILY, 86400000, false, false)) {
            Gamer gamer = client.getGamer();

            gamer.saveProperty(GamerProperty.BALANCE, (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0) + coinsAmount);

            UtilMessage.message(player, COMMAND_PREFIX, "core.command.daily.claimed",
                    Component.text(UtilFormat.formatNumber(coinsAmount)).color(NamedTextColor.GREEN));

        } else {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.daily.already");
        }
    }
}
