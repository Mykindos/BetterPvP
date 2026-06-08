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
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

@Singleton
@CustomLog
public class MessageCommand extends Command {

    private final ClientManager clientManager;
    private final IFilterService filterService;
    private final IIgnoreService ignoreService;

    @Inject
    public MessageCommand(ClientManager clientManager, IFilterService filterService, IIgnoreService ignoreService) {
        this.clientManager = clientManager;
        this.filterService = filterService;
        this.ignoreService = ignoreService;
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
        if (args.length > 1) {

            if (client.hasPunishment(PunishmentTypes.MUTE)) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.muted");
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.player_not_found");
                return;
            }

            if(target.isOp() && target.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.player_not_found");
                return;
            }

            if (player.equals(target)) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.self");
                return;
            }

            Client targetClient = clientManager.search().online(target);
            // check if client has target igored
            if (ignoreService.isClientIgnored(client, targetClient)) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.ignored",
                        Component.text(target.getName(), NamedTextColor.YELLOW));
                return;
            }

            if (!player.isListed(target) && !client.hasRank(Rank.ADMIN)) {
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.player_not_found");
                return;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            filterService.filterMessage(message).thenAccept(filteredMessage -> {
                client.putProperty(ClientProperty.LAST_MESSAGED.name(), target.getUniqueId(), true);
                log.info("{} messaged {}: {}", player.getName(), target.getName(), message).submit();

                // We still pretend the message was sent, regardless of ignore status
                final Component you = Translations.component("core.command.message.you");
                UtilMessage.message(player, privateMessage(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA,
                        you, Component.text(target.getName()), filteredMessage));
                if (!ignoreService.isClientIgnored(targetClient, client)) {
                    UtilMessage.message(target, privateMessage(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA,
                            Component.text(player.getName()), you, filteredMessage));

                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.equals(target) || online.equals(player)) continue;
                        if (clientManager.search().online(online).isAdministrating()) {
                            UtilMessage.message(online, privateMessage(NamedTextColor.DARK_GREEN, NamedTextColor.GREEN,
                                    Component.text(player.getName()), Component.text(target.getName()), filteredMessage));
                        }
                    }

                    targetClient.putProperty(ClientProperty.LAST_MESSAGED.name(), client.getUniqueId(), true);
                }

            });
        } else {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.message.usage");
        }
    }

    @Override
    public String getArgumentType(int index) {
        return index == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }

    /**
     * Builds a private-message display line in the form {@code [from -> to] message}, where the
     * brackets and arrow use {@code bracketColor} and the names use {@code nameColor}.
     */
    static Component privateMessage(NamedTextColor bracketColor, NamedTextColor nameColor,
                                    Component from, Component to, String message) {
        return Component.text("[", bracketColor)
                .append(from.colorIfAbsent(nameColor))
                .append(Component.text(" -> ", bracketColor))
                .append(to.colorIfAbsent(nameColor))
                .append(Component.text("] ", bracketColor))
                .append(Component.text(message, NamedTextColor.GRAY));
    }
}
