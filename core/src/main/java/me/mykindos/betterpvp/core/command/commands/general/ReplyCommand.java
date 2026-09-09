package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.chat.network.PrivateMessages;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@CustomLog
@Singleton
public class ReplyCommand extends Command {

    private final PrivateMessages privateMessages;

    @Inject
    public ReplyCommand(PrivateMessages privateMessages) {
        this.privateMessages = privateMessages;
        aliases.add("r");
    }

    @Override
    public String getName() {
        return "Reply";
    }

    @Override
    public String getDescription() {
        return "core.command.reply.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.usage");
            return;
        }

        if (client.hasPunishment(PunishmentTypes.MUTE)) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.muted");
            return;
        }

        final Optional<UUID> lastMessaged = client.getProperty(ClientProperty.LAST_MESSAGED.name());
        if (lastMessaged.isEmpty()) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.no_target");
            return;
        }

        final String message = String.join(" ", args);
        privateMessages.sendTo(player, client, lastMessaged.get(), message).thenAccept(result -> {
            if (result.getOutcome() == PrivateMessages.Outcome.IGNORING) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.ignored",
                        Component.text(result.getTargetName(), NamedTextColor.YELLOW));
            } else if (result.getOutcome() != PrivateMessages.Outcome.DELIVERED) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.player_not_found");
            }
        });
    }
}
