package me.mykindos.betterpvp.core.resourcepack.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.resourcepack.ResourcePack;
import me.mykindos.betterpvp.core.resourcepack.ResourcePackHandler;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class LoadPackCommand extends Command {

    private final ResourcePackHandler resourcePackHandler;

    @Inject
    public LoadPackCommand(ResourcePackHandler resourcePackHandler) {
        this.resourcePackHandler = resourcePackHandler;
    }

    @Override
    public String getName() {
        return "loadpack";
    }

    @Override
    public String getDescription() {
        return "Load a resource pack by name";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.simpleMessage(player, "Usage: /loadpack <player> <pack>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) return;

        ResourcePack pack = resourcePackHandler.getResourcePack(args[1]);
        if (pack == null) {
            UtilMessage.simpleMessage(player, "Resource pack not found");
            return;
        }

        target.addResourcePack(pack.getUuid(), pack.getUrl(), pack.getHashBytes(), null, true);

    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 0) return super.processTabComplete(sender, args);


        String lowercaseArg = args[args.length - 1].toLowerCase();
        if (getArgumentType(args.length).equals("RESOURCE_PACK")) {
            tabCompletions.addAll(resourcePackHandler.getResourcePacks().keySet().stream().filter(s -> s.toLowerCase().startsWith(lowercaseArg)).toList());
        }

        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        } else if (arg == 2) {
            return "RESOURCE_PACK";
        }
        return ArgumentType.NONE.name();
    }
}
