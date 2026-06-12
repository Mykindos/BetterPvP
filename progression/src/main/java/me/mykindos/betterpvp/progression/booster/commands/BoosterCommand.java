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
        return "progression.command.booster.description";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {
        long remaining = boosterManager.getRemainingTime(player.getUniqueId());
        if (remaining <= 0) {
            UtilMessage.message(player, "core.prefix.booster", "progression.booster.none-active");
            return;
        }

        UtilMessage.message(player, "core.prefix.booster", "progression.booster.remaining",
                Component.text(UtilTime.getTime(remaining, 1), NamedTextColor.GREEN));
    }
}
