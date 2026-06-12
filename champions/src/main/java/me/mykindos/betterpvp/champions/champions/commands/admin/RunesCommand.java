package me.mykindos.betterpvp.champions.champions.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@WithReflection
@CustomLog
public class RunesCommand extends Command {

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;

    @Inject
    public RunesCommand(ItemRegistry itemRegistry, ItemFactory itemFactory) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
    }

    @Singleton
    @Override
    public String getName() {
        return "runes";
    }

    @Override
    public String getDescription() {
        return "champions.command.runes.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "core.prefix.command", getUsage());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            UtilMessage.message(player, "core.prefix.command", "champions.command.runes.invalid-player", Component.text(args[0], NamedTextColor.YELLOW));
            return;
        }

        final List<ItemInstance> runes = itemRegistry.getItems().values().stream()
                .filter(item -> item instanceof RuneItem)
                .map(itemFactory::create)
                .toList();

        for (ItemInstance item : runes) {
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
        return Translations.component("champions.command.runes.usage").color(NamedTextColor.GREEN);
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

}
