package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.item.*;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WithReflection
@CustomLog
public class CustomGiveCommand extends Command {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final ClientManager clientManager;


    @Inject
    public CustomGiveCommand(ItemFactory itemFactory, ItemRegistry itemRegistry, ClientManager clientManager) {
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        this.clientManager = clientManager;
    }

    @Singleton
    @Override
    public String getName() {
        return "customgive";
    }

    @Override
    public String getDescription() {
        return "Give a custom item to a player";
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
;
        BaseItem baseItem = itemRegistry.getItem(args[1]);
        if (baseItem == null) {
            final @NotNull Map<NamespacedKey, BaseItem> options = itemRegistry.getItemsByKey(args[1]);
            if (options.isEmpty()) {
                UtilMessage.message(player, "Command", UtilMessage.deserialize("<green>%s</green> is not a valid item", args[1]));
                return;
            }

            if (options.size() > 1) {
                UtilMessage.message(player, "Command", UtilMessage.deserialize("Found too many matches for key <green>%s</green>, please include a namespace.", args[1]));
                return;
            }

            final Map.Entry<NamespacedKey, BaseItem> entry = options.entrySet().iterator().next();
            baseItem = entry.getValue();
        }

        final NamespacedKey namespacedKey = itemRegistry.getKey(baseItem);
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

        ItemInstance instance = itemFactory.create(baseItem);
        clientManager.sendMessageToRank("Core", UtilMessage.deserialize("<yellow>%s</yellow> gave <yellow>%s</yellow> [<green>%s</green>] x<green>%s</green>",
                player.getName(), target.getName(), namespacedKey, count), Rank.TRIAL_MOD);

        // Give Uncommon+ rarities a UUID
        if (instance.getRarity().isImportant()) {
            Optional<UUIDProperty>  component = instance.getComponent(UUIDProperty.class);
            component.ifPresent(uuidProperty ->
                    log.info("{} spawned and gave ({}) to {}", player.getName(), uuidProperty.getUniqueId(), target.getName())
                            .setAction("ITEM_SPAWN")
                            .addClientContext(player)
                            .addClientContext(target, true)
                            .addItemContext(itemRegistry, instance)
                            .submit());
        }

        final ItemStack itemStack = instance.createItemStack();
        itemStack.setAmount(Math.min(count, itemStack.getMaxStackSize())); // Ensure the amount does not exceed max stack size
        if (itemStack.getAmount() < count) {
            UtilMessage.message(player, "Command", UtilMessage.deserialize("<yellow>Warning:</yellow> <red>Item stack size is limited to <green>%s</green>, giving only x<green>%s</green>.", itemStack.getMaxStackSize(), itemStack.getAmount()));
        }

        target.getInventory().addItem(itemStack);
        clientManager.sendMessageToRank("Core", UtilMessage.deserialize("<yellow>%s</yellow> gave <yellow>%s</yellow> [<green>%s</green>] x<green>%s</green>",
                player.getName(),
                target.getName(),
                namespacedKey,
                itemStack.getAmount()), Rank.HELPER);
        // todo handle items that do not fit in inventory
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
            tabCompletions.addAll(itemRegistry.getItems().keySet().stream()
                    .map(NamespacedKey::toString)
                    .filter(key -> key.toLowerCase().contains(lowercaseArg))
                    .toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
