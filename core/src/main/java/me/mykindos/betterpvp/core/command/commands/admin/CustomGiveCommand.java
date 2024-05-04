package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@WithReflection
@CustomLog
public class CustomGiveCommand extends Command {

    private final ItemHandler itemHandler;
    private final ClientManager clientManager;
    private final UUIDManager uuidManager;


    @Inject
    public CustomGiveCommand(ItemHandler itemHandler, ClientManager clientManager, UUIDManager uuidManager) {
        this.itemHandler = itemHandler;
        this.clientManager = clientManager;
        this.uuidManager = uuidManager;
    }

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
            UtilMessage.message(player, "Command", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid player name.", args[0]));
            return;
        }

        BPvPItem item = itemHandler.getItem(args[1]);
        if (item == null) {
            UtilMessage.message(player, "Command", UtilMessage.deserialize("<green>%s</green> is not a valid item", args[1]));
            return;
        }

        int count = 1;

        if (args.length > 2) {
            try {
                count = Integer.parseInt(args[2]);
                if (count < 1) {
                    UtilMessage.message(player, "Command", UtilMessage.deserialize("<green>%s</green> is not a valid number (must be greater than 1)", count));
                    return;
                }
            } catch (NumberFormatException ignored) {
                UtilMessage.message(player, "Command", UtilMessage.deserialize("<green>%s</green> is not a valid number", args[2]));
                return;
            }
        }

        clientManager.sendMessageToRank("Core", UtilMessage.deserialize("<yellow>%s</yellow> gave <yellow>%s</yellow> [<green>%s</green>] x<green>%s</green>", player.getName(), target.getName(), item.getIdentifier(), count), Rank.HELPER);

        ItemStack itemStack = itemHandler.updateNames(item.getItemStack(count));
        itemHandler.updateNames(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        UUIDItem uuidItem = null;

        if (itemMeta != null) {
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
                uuidItem = uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING)))).orElse(null);
            }
        }
        if (uuidItem != null) {
            log.info("{} spawned and gave ({}) to {}", player.getName(), uuidItem.getUuid(), target.getName())
                    .setAction("ITEM_SPAWN").addClientContext(player).addClientContext(target, true).addItemContext(uuidItem).submit();

        }
        target.getInventory().addItem(itemStack);
        //todo handle items that do not fit in inventory
    }

    public Component getUsage() {
        return UtilMessage.deserialize("<yellow>Usage</yellow>: <green>customgive <player> <item> [amount]");
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        }
        if (arg == 2) {
            return "CUSTOMITEM";
        }
        return ArgumentType.NONE.name();
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 0) return super.processTabComplete(sender, args);;

        String lowercaseArg = args[args.length - 1].toLowerCase();
        if (getArgumentType(args.length).equals("CUSTOMITEM")) {
            tabCompletions.addAll(itemHandler.getItemIdentifiers().stream()
                    .filter(name -> name.toLowerCase().contains(lowercaseArg)).toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
