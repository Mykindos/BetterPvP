package me.mykindos.betterpvp.progression.booster.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.progression.booster.BoosterManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class BoosterCommand extends Command {

    private final BoosterManager boosterManager;

    @Inject
    public BoosterCommand(BoosterManager boosterManager) {
        this.boosterManager = boosterManager;
        setEnabled(true);
    }

    @Override
    public String getName() {
        return "booster";
    }

    @Override
    public String getDescription() {
        return "Check your remaining profession experience booster time";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {
        long remaining = boosterManager.getRemainingTime(player.getUniqueId());
        if (remaining <= 0) {
            UtilMessage.simpleMessage(player, "Booster", "You do not have an active profession experience booster.");
            return;
        }

        UtilMessage.simpleMessage(player, "Booster", Component.text("You have ", NamedTextColor.GRAY)
                .append(Component.text(UtilTime.getTime(remaining, 1), NamedTextColor.GREEN))
                .append(Component.text(" of profession experience booster remaining.", NamedTextColor.GRAY)));
    }
}
