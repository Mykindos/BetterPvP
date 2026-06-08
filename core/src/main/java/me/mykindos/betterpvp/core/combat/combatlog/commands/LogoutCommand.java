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
        return "core.command.logout.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        PlayerCombatLogEvent event = UtilServer.callEvent(new PlayerCombatLogEvent(client, player));
        String result = event.isSafe() ? "safe" : "unsafe";
        UtilMessage.message(player, "core.prefix.logout", "core.command.logout.status",
                me.mykindos.betterpvp.core.locale.Translations.component("core.command.logout." + result)
                        .color(event.isSafe() ? net.kyori.adventure.text.format.NamedTextColor.GREEN : net.kyori.adventure.text.format.NamedTextColor.RED));
    }
}
