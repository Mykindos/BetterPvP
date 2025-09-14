package me.mykindos.betterpvp.core.recipe.menu;

import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipe;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackTabButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipe;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GuiRecipeViewer extends AbstractPagedGui<Gui> implements Windowed {

    public GuiRecipeViewer(@NotNull Collection<Recipe<?, ?>> recipes, @Nullable Windowed previousWindow) {
        super(9, 6, false, new Structure(
                "0XXXXXXX0",
                "0XXXXXXX0",
                "<XXXXXXX>",
                "0XXXXXXX0",
                "0XXXXXXX0",
                "B00000000"
        ).addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', PageBackwardButton.defaultTexture().withDisabledInvisible(true))
                .addIngredient('>', PageForwardButton.defaultTexture().withDisabledInvisible(true))
                .addIngredient('B', new BackTabButton(previousWindow)));

        List<Gui> guis = new ArrayList<>();
        for (Recipe<?, ?> recipe : recipes) {
            guis.add(createGui(recipe));
        }

        setContent(guis);
        addPageChangeHandler((previousPage, nextPage) -> {
            for (Window window : findAllWindows()) {
                window.changeTitle(getTitle(content.get(nextPage)));
            }
        });
    }

    private Gui createGui(Recipe<?, ?> recipe) {
        if (recipe instanceof CastingMoldRecipe castingMoldRecipe) {
            return new GuiCastingRecipeViewer(castingMoldRecipe);
        } else if (recipe instanceof CraftingRecipe craftingRecipe) {
            return new GuiCraftingRecipeViewer(craftingRecipe);
        } else if (recipe instanceof AnvilRecipe anvilRecipe) {
            return new GuiHammeringRecipeViewer(anvilRecipe);
        } else if (recipe instanceof ImbuementRecipe imbuementRecipe) {
            return new GuiImbuingRecipeViewer(imbuementRecipe);
        } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
            return new GuiSmeltingRecipeViewer(smeltingRecipe);
        } else {
            throw new IllegalArgumentException("Unsupported recipe type: " + recipe.getClass().getName());
        }
    }

    @Override
    public void bake() {
        List<List<SlotElement>> pages = new ArrayList<>();
        for (Gui gui : content) {
            List<SlotElement> page = new ArrayList<>(gui.getSize());
            for (int slot = 0; slot < gui.getSize(); slot++) {
                page.add(new SlotElement.LinkedSlotElement(gui, slot));
            }

            pages.add(page);
        }

        this.pages = pages;
        update();
    }

    private Component getTitle(Gui gui) {
        if (gui instanceof Windowed windowed) {
            return windowed.getTitle();
        } else {
            throw new IllegalStateException("No title for this gui");
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return getTitle(content.get(getCurrentPage()));
    }
}
