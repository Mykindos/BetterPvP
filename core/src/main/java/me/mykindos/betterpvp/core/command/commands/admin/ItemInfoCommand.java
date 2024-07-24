package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        if(itemMeta == null) {
            UtilMessage.simpleMessage(player, "Info", "<red>Item has no meta data");
            return;
        }

        if(itemMeta.hasCustomModelData()) {
            UtilMessage.simpleMessage(player, "Info", "<yellow>Custom Model Data: <green>%d", itemMeta.getCustomModelData());
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
