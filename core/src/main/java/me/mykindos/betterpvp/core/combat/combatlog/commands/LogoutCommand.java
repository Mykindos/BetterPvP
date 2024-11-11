package me.mykindos.betterpvp.core.combat.combatlog.commands;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerCombatLogEvent;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

public class LogoutCommand extends Command {
    @Override
    public String getName() {
        return "logout";
    }

    @Override
    public String getDescription() {
        return "Informs the player on whether they are safe to logout";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        PlayerCombatLogEvent event = UtilServer.callEvent(new PlayerCombatLogEvent(client, player));
        String result = event.isSafe() ? "<green>safe</green>" : "<red><b>unsafe</b></red>";
        UtilMessage.message(player, "Logout", "It is currently %s to logout", result);
    }
}
