package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.commands.menu.KitMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class KitCommand extends Command {

    @Inject
    private KitMenu kitMenu;

    @Inject
    @Config(path = "command.kit.kitCooldown", defaultValue = "true")
    private boolean kitCooldown;

    @Inject
    @Config(path = "command.kit.kitCooldownHours", defaultValue = "1")
    private double kitCooldownHours;

    @Inject
    @Config(path = "command.kit.kitCooldownMinutes", defaultValue = "0")
    private double kitCooldownMinutes;

    @Inject
    @Config(path = "command.kit.kitCooldownSeconds", defaultValue = "0")
    private double kitCooldownSeconds;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getName() {
        return "kit";
    }

    @Override
    public String getDescription() {
        return "Equip a kit";
    }

    public long getKitCooldown() {
        if (!kitCooldown) {
            return 0;
        }

        long hoursInMillis = (long) (kitCooldownHours * 60 * 60 * 1000);
        long minutesInMillis = (long) (kitCooldownMinutes * 60 * 1000);
        long secondsInMillis = (long) (kitCooldownSeconds * 1000);

        return hoursInMillis + minutesInMillis + secondsInMillis;
    }



    @Override
    public void execute(Player player, Client client, String... args) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long COOLDOWN_TIME = getKitCooldown();

        if (client.hasRank(Rank.ADMIN)) {
            kitMenu.show(player);
            return;
        }

        if (cooldowns.containsKey(playerId)) {
            long cooldownEnd = cooldowns.get(playerId);
            if (currentTime < cooldownEnd) {
                long timeLeftMillis = cooldownEnd - currentTime;

                long hours = TimeUnit.MILLISECONDS.toHours(timeLeftMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftMillis) % 60;

                player.sendMessage("You must wait <alt2>" + hours + "h, " + minutes + "m, " + seconds + "s</alt2> before using this command again.");
                Component component = Component.text("You must wait ", NamedTextColor.GRAY).append(Component.text(hours, NamedTextColor.GREEN)
                        .append(Component.text("h, ", NamedTextColor.GRAY).append(Component.text(minutes, NamedTextColor.GREEN)
                        .append(Component.text("m, ", NamedTextColor.GRAY).append(Component.text(seconds, NamedTextColor.GREEN)
                        .append(Component.text("s, ", NamedTextColor.GRAY)))))));

                UtilMessage.message(player, "Clans", component);
                return;
            }
        }

        kitMenu.show(player);
        long cooldownEnd = currentTime + COOLDOWN_TIME;
        cooldowns.put(playerId, cooldownEnd);
    }

    @UpdateEvent(delay = 60000) //every minute it checks
    public void updateCooldowns() {
        long currentTime = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }
}
