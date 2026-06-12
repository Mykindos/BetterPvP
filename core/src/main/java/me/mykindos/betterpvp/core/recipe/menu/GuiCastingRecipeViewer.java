package me.mykindos.betterpvp.core.recipe.menu;

import com.google.inject.Injector;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.PaginatedLoreItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.menu.viewer.AlloyButton;
import me.mykindos.betterpvp.core.item.menu.viewer.ItemButton;
import me.mykindos.betterpvp.core.locale.Translations;
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
import java.util.List;

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


        setItem(12, new ItemButton(itemFactory.createPreview(recipe.getBaseMold())));
        setItem(19, new AlloyButton(recipe.getAlloy(), recipe.getRequiredMillibuckets(), false, Translations.component("core.menu.alloy.prefix.required")));
        setItem(26, new ItemButton(itemFactory.createPreview(recipe.getResult())));

        setItem(15, new PaginatedLoreItem(recipe.previewResult(), null));
        setItem(5, InfoTabButton.builder()
                // todo: wiki entry
                .icon(itemFactory.createPreview(itemFactory.getItemRegistry().getItem("core:smelter")).createItemStack())
                .wikiEntry("Test", url)
                .descriptionLines(List.of(Translations.rawComponentLines("core.menu.recipe.casting.info.description")))
                .build());
        setBackground(Menu.INVISIBLE_BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_recipe_viewer_casting>").font(NEXO);
    }

}
