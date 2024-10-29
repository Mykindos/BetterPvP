package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
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
        return "Displays hidden information for this item";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = itemInMainHand.getItemMeta();

        if (itemMeta == null) {
            UtilMessage.simpleMessage(player, "Info", "<red>Item has no meta data");
            return;
        }

        if (itemMeta.hasDisplayName()) {
            UtilMessage.message(player, Component.text("Info>", NamedTextColor.BLUE)
                    .append(Component.text(" Name: ", NamedTextColor.YELLOW).append(Objects.requireNonNull(itemMeta.displayName()))));
        }

        if (itemMeta.hasCustomModelData()) {
            UtilMessage.simpleMessage(player, "Info", "<yellow>Custom Model Data: <green>%d", itemMeta.getCustomModelData());
        }

        if (itemMeta instanceof Damageable damageable) {
            if (damageable.hasMaxDamage()) {
                UtilMessage.simpleMessage(player, "Info", "<yellow>Max Durability: <green>%s", damageable.getMaxDamage());
            }
            if (damageable.hasDamageValue()) {
                UtilMessage.simpleMessage(player, "Info", "<yellow>Durability: <green>%s", damageable.getMaxDamage() - damageable.getDamage());
            }
        }

        CraftPersistentDataContainer persistentData = (CraftPersistentDataContainer) itemMeta.getPersistentDataContainer();
        if (persistentData.getKeys().isEmpty()) return;

        persistentData.getRaw().forEach((key, value) -> {
            UtilMessage.simpleMessage(player, "Info", "<yellow>%s: <gray>%s", key, value.toString());
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
