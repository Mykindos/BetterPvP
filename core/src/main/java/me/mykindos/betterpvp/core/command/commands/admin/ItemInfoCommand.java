package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

@Singleton
public class ItemInfoCommand extends Command {


    @Override
    public String getName() {
        return "iteminfo";
    }

    @Override
    public String getDescription() {
        return "core.command.item-info.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = itemInMainHand.getItemMeta();

        UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.type", itemInMainHand.getType().name());
        if (itemMeta == null) {
            UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.no_meta");
            return;
        }

        if (itemMeta.hasDisplayName()) {
            UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.name", Objects.requireNonNull(itemMeta.displayName()));
        }

        if (itemMeta.hasCustomModelData()) {
            UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.model_data", itemMeta.getCustomModelData());
        }

        if (itemInMainHand.hasData(DataComponentTypes.MAX_DAMAGE)) {
            int maxDamage = itemInMainHand.getData(DataComponentTypes.MAX_DAMAGE);
            UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.max_durability", maxDamage);

            if (itemInMainHand.hasData(DataComponentTypes.DAMAGE)) {
                int damage = itemInMainHand.getData(DataComponentTypes.DAMAGE);
                UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.durability", maxDamage - damage);
            }
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("true")) {
            for (DataComponentType dataType : itemInMainHand.getDataTypes()) {
                if (dataType instanceof DataComponentType.Valued<?> valued) {
                    UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.data_entry",
                            Component.text(String.valueOf(dataType.getKey()), NamedTextColor.YELLOW),
                            Component.text(itemInMainHand.getData(valued).toString(), NamedTextColor.GRAY));
                } else {
                    UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.data_entry",
                            Component.text(String.valueOf(dataType.getKey()), NamedTextColor.YELLOW),
                            Translations.component("core.command.iteminfo.true").color(NamedTextColor.GREEN));
                }
            }
        }

        CraftPersistentDataContainer persistentData = (CraftPersistentDataContainer) itemMeta.getPersistentDataContainer();
        if (persistentData.getKeys().isEmpty()) return;

        persistentData.getRaw().forEach((key, value) -> {
            UtilMessage.message(player, "core.prefix.command", "core.command.iteminfo.data_entry",
                    Component.text(String.valueOf(key), NamedTextColor.YELLOW),
                    Component.text(value.toString(), NamedTextColor.GRAY));
        });
    }


    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public String getArgumentType(int argCount) {
        return ArgumentType.NONE.name();
    }

}
