package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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
        return "View the discord invite link";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Discord", "<yellow><click:open_url:'%s'>Click Here</click></yellow> to join our discord!", discordInvite);
    }
}
