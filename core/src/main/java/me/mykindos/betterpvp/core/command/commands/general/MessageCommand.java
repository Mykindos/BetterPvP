package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.chat.network.PrivateMessages;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@Singleton
@CustomLog
public class MessageCommand extends Command {

    private final PrivateMessages privateMessages;

    @Inject
    public MessageCommand(PrivateMessages privateMessages) {
        this.privateMessages = privateMessages;
        aliases.addAll(List.of("m", "msg", "tell", "whisper", "w"));
    }

    @Override
    public String getName() {
        return "message";
    }

    @Override
    public String getDescription() {
        return "core.command.message.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length <= 1) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.usage");
            return;
        }

        if (client.hasPunishment(PunishmentTypes.MUTE)) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.muted");
            return;
        }

        final String targetName = args[0];
        final String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        privateMessages.send(player, client, targetName, message).thenAccept(result -> {
            switch (result.getOutcome()) {
                case NOT_FOUND -> UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.player_not_found");
                case SELF -> UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.self");
                case IGNORING -> UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.ignored",
                        Component.text(result.getTargetName(), NamedTextColor.YELLOW));
                case DELIVERED -> {
                }
            }
        });
    }

    @Override
    public String getArgumentType(int index) {
        return index == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
