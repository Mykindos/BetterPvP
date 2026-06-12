package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        return "give";
    }

    @Override
    public String getDescription() {
        return "core.command.custom-give.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "core.prefix.command", getUsage());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            UtilMessage.message(player, "core.prefix.command", "core.command.give.invalid_player", Component.text(args[0], NamedTextColor.RED));
            return;
        }
;
        BaseItem baseItem = itemRegistry.getItem(args[1]);
        if (baseItem == null) {
            final @NotNull Map<NamespacedKey, BaseItem> options = itemRegistry.getItemsByKey(args[1]);
            if (options.isEmpty()) {
                UtilMessage.message(player, "core.prefix.command", "core.command.give.invalid_item", Component.text(args[1], NamedTextColor.GREEN));
                return;
            }

            if (options.size() > 1) {
                UtilMessage.message(player, "core.prefix.command", "core.command.give.too_many_matches", Component.text(args[1], NamedTextColor.RED));
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
                    UtilMessage.message(player, "core.prefix.command", "core.command.give.amount_positive", Component.text(count, NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException ignored) {
                UtilMessage.message(player, "core.prefix.command", "core.command.give.invalid_number", Component.text(args[2], NamedTextColor.RED));
                return;
            }
        }

        int toGive = count;
        while (toGive > 0) {
            final ItemInstance instance = itemFactory.create(baseItem);
            final ItemStack itemStack = instance.createItemStack();
            final int stackSize = itemStack.getMaxStackSize();
            int giveAmount = Math.min(toGive, stackSize);
            itemStack.setAmount(giveAmount);
            UtilItem.insert(target, itemStack);

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

            toGive -= giveAmount;
        }

        clientManager.sendMessageToRank("core.prefix.core", Translations.component("core.command.give.success",
                Component.text(player.getName(), NamedTextColor.YELLOW),
                Component.text(target.getName(), NamedTextColor.YELLOW),
                Component.text(namespacedKey.toString(), NamedTextColor.GREEN),
                Component.text(count, NamedTextColor.GREEN)), Rank.HELPER);
    }

    public Component getUsage() {
        return Translations.component("core.command.give.usage");
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
            tabCompletions.addAll(itemRegistry.getItemsSorted().keySet().stream()
                    .map(NamespacedKey::toString)
                    .filter(key -> key.toLowerCase().contains(lowercaseArg))
                    .toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
