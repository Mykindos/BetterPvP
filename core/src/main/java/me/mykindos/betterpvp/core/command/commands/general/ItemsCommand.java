package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.menu.ItemViewMenu;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
public class ItemsCommand extends Command {
    private final ItemHandler itemHandler;

    @Inject
    public ItemsCommand(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
        this.aliases.addAll(List.of(
                "items",
                "weapon",
                "weapons",
                "customitem",
                "customitems",
                "customweapon",
                "customweapons"
        ));
    }

    @Override
    public String getName() {
        return "item";
    }

    @Override
    public String getDescription() {
        return "View all items";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new ItemViewMenu("Items", itemHandler, null).show(player);
    }
}
