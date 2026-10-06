package me.mykindos.betterpvp.core.resourcepack.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.resourcepack.ResourcePackListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class ResendPacksCommand extends Command {

    private final ResourcePackListener sender;

    @Inject
    public ResendPacksCommand(ResourcePackListener sender) {
        this.sender = sender;
    }

    @Override
    public String getName() {
        return "resendpacks";
    }

    @Override
    public String getDescription() {
        return "core.command.resendpacks.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        if (target == null) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.resendpacks.usage");
            return;
        }
        sender.resend(target);
        UtilMessage.message(player, COMMAND_PREFIX, "core.command.resendpacks.success", Component.text(target.getName()));
    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

}
