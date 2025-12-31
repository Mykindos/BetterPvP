package me.mykindos.betterpvp.core.item.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.menu.viewer.GuiItemViewer;
import org.bukkit.entity.Player;

@Singleton
public class ItemsCommand extends Command {

    private final ItemFactory itemFactory;

    @Inject
    public ItemsCommand(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
        this.aliases.add("recipes");
        this.aliases.add("recipe");
    }

    @Override
    public String getName() {
        return "items";
    }

    @Override
    public String getDescription() {
        return "View all items and their recipes";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new GuiItemViewer(itemFactory).show(player);
    }
}
