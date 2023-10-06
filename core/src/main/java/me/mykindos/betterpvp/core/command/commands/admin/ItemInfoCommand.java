package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

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

        var persistentData = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer();
        if (persistentData.getKeys().isEmpty()) return;
        persistentData.getKeys().forEach(key -> {
            UtilMessage.simpleMessage(player, "Info", "<yellow>%s: <gray>%s", key.asString(), persistentData.getOrDefault(key, PersistentDataType.STRING, ""));
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
