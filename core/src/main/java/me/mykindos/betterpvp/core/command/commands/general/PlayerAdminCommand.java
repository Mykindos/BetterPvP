package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class PlayerAdminCommand extends Command {

    private final ClientManager clientManager;
    private final CooldownManager cooldownManager;

    @Inject
    public PlayerAdminCommand(ClientManager clientManager, CooldownManager cooldownManager){
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
        if(args.length == 0) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }

        if (client.hasPunishment(PunishmentTypes.MUTE)) {
            if (!cooldownManager.use(player, getName(), 120, false, false)) {
                UtilMessage.message(player, "Core", "You must wait 2 minutes between using this command.");
                return;
            }
        }

        String playerName = UtilFormat.spoofNameForLunar(player.getName());
        Rank sendRank = client.getRank();
        Component senderComponent = Component.text(playerName, sendRank.getColor()).hoverEvent(HoverEvent.showText(Component.text(sendRank.getName(), sendRank.getColor())));
        Component message = Component.text(" " + String.join(" ", args), NamedTextColor.LIGHT_PURPLE);
        //Start with a Component.empty() to avoid the hoverEvent from propagating down
        Component component = Component.empty().append(senderComponent).append(message);
        if (!client.hasRank(Rank.HELPER)) {
            //dont send the message twice to a staff member
            UtilMessage.message(player, component);
            UtilMessage.message(player, "Core", "If a staff member is on this server, they have received this message");
        }

        clientManager.sendMessageToRank("", component, Rank.HELPER);
    }
}
