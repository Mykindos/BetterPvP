package me.mykindos.betterpvp.core.recipe.menu;

import com.google.inject.Injector;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.menu.viewer.AlloyButton;
import me.mykindos.betterpvp.core.item.menu.viewer.ItemButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class GuiCastingRecipeViewer extends AbstractGui implements Windowed {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public GuiCastingRecipeViewer(CastingMoldRecipe recipe) {
        super(7, 5);
        final Injector injector = JavaPlugin.getPlugin(Core.class).getInjector();
        ItemFactory itemFactory = injector.getInstance(ItemFactory.class);


        setItem(12, new ItemButton(itemFactory.create(recipe.getBaseMold())));
        setItem(19, new AlloyButton(recipe.getAlloy(), recipe.getRequiredMillibuckets(), false, "Required"));
        setItem(26, new ItemButton(itemFactory.create(recipe.getResult())));

        setItem(15, new SimpleItem(recipe.createPrimaryResult().createItemStack()));
        setItem(5, InfoTabButton.builder()
                // todo: wiki entry
                .icon(itemFactory.create(itemFactory.getItemRegistry().getItem("core:smelter")).createItemStack())
                .wikiEntry("Test", url)
                .description(Component.text("Click on an ingredient to look at its recipes. Casting recipes require an alloy to be stored in the smelter first. Click on the alloy to view its recipe."))
                .build());
        setBackground(Menu.INVISIBLE_BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_recipe_viewer_casting>").font(NEXO);
    }

}
