package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@CustomLog
@Singleton
public class ReplyCommand extends Command {

    private final ClientManager clientManager;
    private final IFilterService filterService;
    private final IIgnoreService ignoreService;

    @Inject
    public ReplyCommand(ClientManager clientManager, IFilterService filterService, IIgnoreService ignoreService) {
        this.clientManager = clientManager;
        this.filterService = filterService;
        this.ignoreService = ignoreService;
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

        Optional<UUID> lastMessagedOptional = client.getProperty(ClientProperty.LAST_MESSAGED.name());
        if (lastMessagedOptional.isEmpty()) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.no_target");
            return;
        }

        UUID lastMessaged = lastMessagedOptional.get();
        Player target = Bukkit.getPlayer(lastMessaged);
        if (target == null) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.player_not_found");
            return;
        }

        Client targetClient = clientManager.search().online(target);

        if (!player.isListed(target) && !client.hasRank(Rank.ADMIN)) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.player_not_found");
            return;
        }

        String message = String.join(" ", args);
        filterService.filterMessage(message).thenAccept(filteredMessage -> {
            boolean isClientIgnored = ignoreService.isClientIgnored(targetClient, client);
            boolean isTargetIgnored = ignoreService.isClientIgnored(client, targetClient);

            if (isTargetIgnored) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.reply.ignored",
                        Component.text(target.getName(), NamedTextColor.YELLOW));
                return;
            }

            final Component you = Translations.component("core.command.message.you");

            if (isClientIgnored) {
                // We still send a fake message
                UtilMessage.message(player, MessageCommand.privateMessage(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA,
                        you, Component.text(target.getName()), filteredMessage));
                client.putProperty(ClientProperty.LAST_MESSAGED.name(), target.getUniqueId(), true);
                return;
            }


            UtilMessage.message(player, MessageCommand.privateMessage(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA,
                    you, Component.text(target.getName()), filteredMessage));
            UtilMessage.message(target, MessageCommand.privateMessage(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA,
                    Component.text(player.getName()), you, filteredMessage));

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(target) || online.equals(player)) continue;
                if (clientManager.search().online(online).isAdministrating()) {
                    UtilMessage.message(online, MessageCommand.privateMessage(NamedTextColor.DARK_GREEN, NamedTextColor.GREEN,
                            Component.text(player.getName()), Component.text(target.getName()), filteredMessage));
                }
            }

            client.putProperty(ClientProperty.LAST_MESSAGED.name(), target.getUniqueId(), true);
            targetClient.putProperty(ClientProperty.LAST_MESSAGED.name(), client.getUniqueId(), true);

            log.info(player.getName() + " messaged " + target.getName() + ": " + message).submit();

        });

    }
}
