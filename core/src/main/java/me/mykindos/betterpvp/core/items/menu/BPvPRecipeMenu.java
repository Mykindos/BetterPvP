package me.mykindos.betterpvp.core.items.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BPvPRecipeMenu extends AbstractGui implements Windowed {
    final BPvPItem item;
    final List<Recipe> recipes = new ArrayList<>();
    int page = 0;

    public BPvPRecipeMenu(BPvPItem item, Windowed previous) {
        super(9, 5);
        this.item = item;
        if (!item.getRecipeKeys().isEmpty()) {
            recipes.addAll(
                    item.getRecipeKeys().stream()
                            .map(Bukkit::getRecipe)
                            .toList()
            );
        }
        Structure structure = new Structure("XXXXXXXXX",
                "XXCCCXXXX",
                "XXCCCXRXX",
                "XXCCCXXXX",
                "XXXPBNXXX");
        structure.addIngredient('X', Menu.BACKGROUND_ITEM);
        structure.addIngredient('C', ItemProvider.EMPTY);
        structure.addIngredient('R', ItemProvider.EMPTY);
        structure.addIngredient('P', new PreviousRecipeButton());
        structure.addIngredient('B', new BackButton(previous));
        structure.addIngredient('N', new NextRecipeButton());
        applyStructure(structure);
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
                setItem(x + 2, y + 1, getItem(recipeChoice));
            }
        }
        ItemView result = ItemView.of(shapedRecipe.getResult());
        setItem(6, 2, result.toSimpleItem());
    }

    private Item getItem(RecipeChoice recipeChoice) {
        if (recipeChoice instanceof RecipeChoice.ExactChoice exactChoice) {
            ItemView itemView = ItemView.of(exactChoice.getItemStack());
            return itemView.toSimpleItem();
        }
        if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
            ItemView itemView = ItemView.of(materialChoice.getItemStack());
            return itemView.toSimpleItem();
        }
        return ItemView.of(ItemStack.of(Material.AIR)).toSimpleItem();
    }

    private void setShapelessGui(ShapelessRecipe shapelessRecipe) {
        int i = 0;
        for (int y = 0; y < 3; y ++) {
            for (int x = 0; x < 3; x ++) {
                setItem(x + 2, y + 1, getItem(shapelessRecipe.getChoiceList().get(i)));
                i++;
                if (i >= shapelessRecipe.getChoiceList().size()) {
                    break;
                }
            }
            if (i >= shapelessRecipe.getChoiceList().size()) {
                break;
            }
        }
        ItemView result = ItemView.of(shapelessRecipe.getResult());
        setItem(6, 2, result.toSimpleItem());
    }


    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text(item.getKey());
    }
}
