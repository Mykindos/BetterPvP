package me.mykindos.betterpvp.core.recipe.menu;

import com.google.inject.Injector;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipe;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class GuiImbuingRecipeViewer extends AbstractGui implements Windowed {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public GuiImbuingRecipeViewer(ImbuementRecipe recipe) {
        super(7, 5);
        final Injector injector = JavaPlugin.getPlugin(Core.class).getInjector();
        ItemFactory itemFactory = injector.getInstance(ItemFactory.class);

        for (int x = 4; x <= 6; x++) {
            for (int y = 1; y <= 3; y++) {
                int index = (y - 1) * 3 + x - 4;
                final RecipeIngredient ingredient = recipe.getIngredients().get(index);
                if (ingredient == null) {
                    continue;
                }

                final BaseItem baseItem = ingredient.getBaseItem();
                final int amount = ingredient.getAmount();
                final ItemInstance instance = itemFactory.create(baseItem);
                instance.getItemStack().setAmount(amount);
                setItem(x, y, new ItemButton(instance));
            }
        }

        // Result
        setItem(15, new SimpleItem(recipe.createPrimaryResult().createItemStack()));
        setItem(5, InfoTabButton.builder()
                // todo: wiki entry
                .icon(itemFactory.create(itemFactory.getItemRegistry().getItem("core:imbuement_pedestal")).createItemStack())
                .wikiEntry("Test", url)
                .description(Component.text("Click on an ingredient to look at its recipes. To imbue an item, place its ingredients in the imbuement pedestal and right-click it to start imbuing."))
                .build());
        setBackground(Menu.INVISIBLE_BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_recipe_viewer_imbuing>").font(NEXO);
    }
}
