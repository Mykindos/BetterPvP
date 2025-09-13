package me.mykindos.betterpvp.core.recipe.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.menu.GuiCraftingRecipeViewer;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
@Singleton
public class RecipeCommand extends Command {

    private final ItemFactory itemFactory;
    private final CraftingRecipeRegistry registry;

    @Inject
    public RecipeCommand(ItemFactory itemFactory, CraftingRecipeRegistry registry) {
        this.itemFactory = itemFactory;
        this.registry = registry;
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
//        new GuiCraftingRecipeViewer(recipes, () -> open(player)).show(player);
    }
}
