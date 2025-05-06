package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.mineplex.MineplexMessage;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageReceivedEvent;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageSentEvent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class PlayerAdminCommand extends Command {

    private final Core core;
    private final ClientManager clientManager;
    private final CooldownManager cooldownManager;

    @Inject
    public PlayerAdminCommand(Core core, ClientManager clientManager, CooldownManager cooldownManager) {
        this.core = core;
        this.clientManager = clientManager;
        this.cooldownManager = cooldownManager;

        aliases.add("a");
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription() {
        return "Send a message to staff";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }

        if (client.hasPunishment(PunishmentTypes.MUTE)) {
            if (!cooldownManager.use(player, getName(), 120, false, false)) {
                UtilMessage.message(player, "Core", "You must wait 2 minutes between using this command.");
                return;
            }
        }

        MineplexMessage build = MineplexMessage.builder().channel("AdminMessage").message(String.join(" ", args))
                .metadata("sender", player.getUniqueId().toString()).build();
        if (Bukkit.getPluginManager().getPlugin("StudioEngine") != null) {
            UtilServer.callEventAsync(core, new MineplexMessageSentEvent("BetterPvP", build));
        } else {
            UtilServer.callEventAsync(core, new MineplexMessageReceivedEvent("BetterPvP", build));
        }


    }
}
