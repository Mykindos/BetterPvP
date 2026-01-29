package me.mykindos.betterpvp.core.item.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.menu.viewer.GuiItemViewer;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import org.bukkit.entity.Player;

@Singleton
public class ItemsCommand extends Command {

    private final ItemFactory itemFactory;
    private final RecipeRegistries recipeRegistries;

    @Inject
    public ItemsCommand(ItemFactory itemFactory, RecipeRegistries recipeRegistries) {
        this.itemFactory = itemFactory;
        this.recipeRegistries = recipeRegistries;
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
        new GuiItemViewer(itemFactory, recipeRegistries).show(player);
    }
}
