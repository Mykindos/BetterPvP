package me.mykindos.betterpvp.core.item.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.persistence.PersistentDataContainerView;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Singleton
public class MockItemCommand extends Command {

    private final ItemFactory itemFactory;
    private final ItemRegistry registry;

    @Inject
    public MockItemCommand(ItemFactory itemFactory, ItemRegistry registry) {
        this.itemFactory = itemFactory;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "mockitem";
    }

    @Override
    public String getDescription() {
        return "Mock an item by setting your currently held item to a base item's ID";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 1) {
            UtilMessage.message(player, "<yellow>Usage<gray>: <green>/mockitem <Item ID>");
            return;
        }

        final ItemStack mainHand = player.getEquipment().getItemInMainHand();
        if (mainHand.getType().isAir()) {
            UtilMessage.message(player, "<red>You must be holding an item to use it as a mock.");
            return;
        }

        BaseItem baseItem = registry.getItem(args[0]);
        if (baseItem == null) {
            final @NotNull Map<NamespacedKey, BaseItem> options = registry.getItemsByKey(args[0]);
            if (options.isEmpty()) {
                UtilMessage.message(player, "Command", UtilMessage.deserialize("<green>%s</green> is not a valid item", args[0]));
                return;
            }

            if (options.size() > 1) {
                UtilMessage.message(player, "Command", UtilMessage.deserialize("Found too many matches for key <green>%s</green>, please include a namespace.", args[0]));
                return;
            }

            final Map.Entry<NamespacedKey, BaseItem> entry = options.entrySet().iterator().next();
            baseItem = entry.getValue();
        }

        final ItemInstance newInstance = itemFactory.create(baseItem);
        final PersistentDataContainerView pdc = newInstance.getItemStack().getPersistentDataContainer();

        final ItemMeta meta = mainHand.getItemMeta();
        pdc.copyTo(meta.getPersistentDataContainer(), true);
        mainHand.setItemMeta(meta);
        player.getInventory().setItemInMainHand(mainHand);
        UtilMessage.message(player, "Mock", "<green>Successfully mocked item to <yellow>%s<green>.", registry.getKey(baseItem).toString());
    }
}
