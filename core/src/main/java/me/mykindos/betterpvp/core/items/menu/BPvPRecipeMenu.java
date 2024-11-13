package me.mykindos.betterpvp.core.items.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BPvPRecipeMenu extends AbstractGui implements Windowed {
    BPvPItem item;
    List<Recipe> recipes = new ArrayList<>();
    int page = 0;

    public BPvPRecipeMenu(BPvPItem item, Windowed previous) {
        super(9, 4);
        this.item = item;
        if (!item.getRecipeKeys().isEmpty()) {
            recipes.addAll(
                    item.getRecipeKeys().stream()
                            .map(Bukkit::getRecipe)
                            .toList()
            );
        }


        /*"789XX"
          "456XR"
          "123XX"
          "LXBXR"*/

        setItem(0, 0, new PreviousRecipeButton());
        setItem(2, 0, new BackButton((previous)));
        setItem(4, 0, new NextRecipeButton());
        updateRecipe();
    }

    public void nextRecipe() {
        if (recipes.isEmpty()) {
            return;
        }
        if (page >= recipes.size()) {
            page = 0;
        }
        updateRecipe();
    }

    public void previousRecipe() {
        if (recipes.isEmpty()) {
            return;
        }
        if (page >= recipes.size()) {
            page = recipes.size() - 1;
        }
        updateRecipe();
    }

    public void updateRecipe() {
        Recipe currentRecipe = recipes.get(page);
        if (currentRecipe instanceof ShapedRecipe shapedRecipe) {
            setShapedGui(shapedRecipe);
        }
        if (currentRecipe instanceof ShapelessRecipe shapelessRecipe) {
            setShapelessGui(shapelessRecipe);
        }
    }

    private void setShapedGui(ShapedRecipe shapedRecipe) {
        shapedRecipe.getChoiceMap();
        for (int y = 0; y < shapedRecipe.getShape().length; y++) {
            char[] row = shapedRecipe.getShape()[y].toCharArray();
            for (int x = 0; x < row.length; x++) {

                RecipeChoice recipeChoice = shapedRecipe.getChoiceMap().get(row[x]);
                if (recipeChoice instanceof RecipeChoice.ExactChoice exactChoice) {
                    ItemView itemView = ItemView.of(exactChoice.getItemStack());
                    setItem(x, y+1, itemView.toSimpleItem());
                }
                if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                    ItemView itemView = ItemView.of(materialChoice.getItemStack());
                    setItem(x, y+1, itemView.toSimpleItem());
                }

            }
        }
        ItemView result = ItemView.of(shapedRecipe.getResult());
        setItem(4, 2, result.toSimpleItem());
    }

    private void setShapelessGui(ShapelessRecipe shapelessRecipe) {
        ItemView result = ItemView.of(shapelessRecipe.getResult());
        setItem(4, 2, result.toSimpleItem());
    }


    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text(item.getKey());
    }
}
