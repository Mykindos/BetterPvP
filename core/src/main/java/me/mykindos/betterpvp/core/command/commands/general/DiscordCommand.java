package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class DiscordCommand extends Command {

    @Inject
    @Config(path = "discord.invite", defaultValue = "https://discord.gg/betterpvp")
    private String discordInvite;

    @Override
    public String getName() {
        return "discord";
    }

    @Override
    public String getDescription() {
        return "core.command.discord.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player,
                "core.prefix.command",
                "core.command.discord.message",
                Translations.component("core.command.discord.message.click")
                        .color(NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.openUrl(discordInvite)));
    }
}
