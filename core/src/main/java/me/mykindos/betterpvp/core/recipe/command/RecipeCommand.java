package me.mykindos.betterpvp.core.recipe.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.menu.GuiCraftingRecipeViewer;
import me.mykindos.betterpvp.core.recipe.menu.GuiItemViewer;
import me.mykindos.betterpvp.core.recipe.menu.GuiRecipeViewer;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
@Singleton
public class RecipeCommand extends Command {

    private final ItemFactory itemFactory;

    @Inject
    public RecipeCommand(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
        this.aliases.add("recipes");
    }

    @Override
    public String getName() {
        return "recipe";
    }

    @Override
    public String getDescription() {
        return "View all crafting recipes";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new GuiItemViewer(itemFactory).show(player);
    }
}
