package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomGiveCommand extends Command {

    @Inject
    ItemHandler itemHandler;

    @Singleton
    @Override
    public String getName() {
        return "customgive";
    }

    @Override
    public String getDescription() {
        return "give a custom item to a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "Command", getUsage());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            UtilMessage.message(player, "command", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid player name.", args[0]));
            return;
        }

        BPVPItem item = itemHandler.getItem(args[1]);
        if (item == null) {
            UtilMessage.message(player, "command", UtilMessage.deserialize("<green>%s</green> is not a valid item", args[1]));
            return;
        }

        int count = 1;

        ItemStack itemStack = itemHandler.updateNames(item.getItemStack(count));
        target.getInventory().addItem(itemStack);
        //todo handle items that do not fit in inventory
    }

    public Component getUsage() {
        return UtilMessage.deserialize("<yellow>Usage</yellow>: <green>customgive <player> <item>");
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        }
        if (arg == 2) {
            return ArgumentType.CUSTOMITEM.name();
        }
        return ArgumentType.NONE.name();
    }
}
