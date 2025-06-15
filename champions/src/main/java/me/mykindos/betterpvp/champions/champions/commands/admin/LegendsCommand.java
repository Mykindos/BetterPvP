package me.mykindos.betterpvp.champions.champions.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.UUIDProperty;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@WithReflection
@CustomLog
public class LegendsCommand extends Command {

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;

    @Inject
    public LegendsCommand(ItemRegistry itemRegistry, ItemFactory itemFactory) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
    }

    @Singleton
    @Override
    public String getName() {
        return "legends";
    }

    @Override
    public String getDescription() {
        return "give the target all legendary and mythic weapons";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Command", getUsage());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            UtilMessage.message(player, "Command", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid player name.", args[0]));
            return;
        }

        final List<ItemInstance> legends = itemRegistry.getItems().values().stream()
                .filter(item -> item instanceof WeaponItem)
                .map(itemFactory::create)
                .filter(item -> item.getRarity().isImportant())
                .toList();

        for (ItemInstance item : legends) {
            ItemStack itemStack = item.createItemStack();

            item.getComponent(UUIDProperty.class).ifPresent(uuidProperty -> {
                UUID uuid = uuidProperty.getUniqueId();
                log.info("{} spawned and gave ({}) to {}", player.getName(), uuid, target.getName())
                        .setAction("ITEM_SPAWN")
                        .addClientContext(player)
                        .addClientContext(target, true)
                        .addItemContext(itemRegistry, item)
                        .submit();
            });

            target.getInventory().addItem(itemStack);
        }
        }

    public Component getUsage() {
        return UtilMessage.deserialize("<yellow>Usage</yellow>: <green>legends <player>");
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

}
