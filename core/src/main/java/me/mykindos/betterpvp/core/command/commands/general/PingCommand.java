package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Singleton
public class PingCommand extends Command {

    @Inject
    @Config(path = "ping.onlyStaffCanSeeOthers", defaultValue = "true")
    private boolean onlyStaffCanSeeOthers;

    private final ClientManager clientManager;

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Show your ping";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.simpleMessage(player, "Ping", "Your ping is %s.", this.formatPing(UtilPlayer.getPing(player)));
            return;
        }

        if (args.length == 1) {
            if (this.onlyStaffCanSeeOthers && !client.hasRank(Rank.MODERATOR)) {
                return;
            }

            this.clientManager.search(player).online(args[0]).ifPresent(targetClient -> {
                Player targetPlayer = targetClient.getGamer().getPlayer();
                if (targetPlayer == null) {
                    return;
                }

                UtilMessage.simpleMessage(player, "Ping", "The ping for <yellow>%s</yellow> is %s.", targetClient.getName(), this.formatPing(UtilPlayer.getPing(targetPlayer)));
            });
        }
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }

    private String formatPing(final int ping) {
        NamedTextColor namedTextColor = NamedTextColor.DARK_RED;

        if (ping <= 0) {
            namedTextColor = NamedTextColor.WHITE;
        } else if (ping < 25) {
            namedTextColor = NamedTextColor.DARK_GREEN;
        } else if (ping < 50) {
            namedTextColor = NamedTextColor.GREEN;
        } else if (ping < 100) {
            namedTextColor = NamedTextColor.GOLD;
        } else if (ping < 200) {
            namedTextColor = NamedTextColor.YELLOW;
        } else if (ping < 300) {
            namedTextColor = NamedTextColor.RED;
        }

        TextComponent textComponent = Component.text(ping).color(namedTextColor).append(Component.text("ms").color(NamedTextColor.GRAY));

        return UtilMessage.miniMessage.serialize(textComponent);
    }
}