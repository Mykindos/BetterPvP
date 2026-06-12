package me.mykindos.betterpvp.core.recipe.menu;

import com.google.inject.Injector;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.PaginatedLoreItem;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.menu.viewer.ItemButton;
import me.mykindos.betterpvp.core.locale.Translations;
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
import java.util.List;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class GuiHammeringRecipeViewer extends AbstractGui implements Windowed {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public GuiHammeringRecipeViewer(AnvilRecipe recipe) {
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
                final ItemInstance instance = itemFactory.createPreview(baseItem);
                instance.getItemStack().setAmount(amount);
                setItem(x, y, new ItemButton(instance));
            }
        }

        // Hammer
        final ItemInstance hammer = itemFactory.createPreview(itemFactory.getItemRegistry().getItem("core:hammer"));
        final TextComponent hammerName = Component.text(recipe.getHammerSwings(), NamedTextColor.YELLOW)
                .appendSpace()
                .append(Translations.component("core.menu.recipe.hammering.swings.label").color(NamedTextColor.GREEN));
        setItem(33, new SimpleItem(ItemView.of(hammer.getView().get()).toBuilder()
                .amount(recipe.getRequiredHammerSwings())
                .displayName(hammerName)
                .build()));

        // Result
        setItem(15, new PaginatedLoreItem(recipe.previewResult().getPrimaryResult(), null));
        setItem(5, InfoTabButton.builder()
                // todo: wiki entry
                .icon(itemFactory.createPreview(itemFactory.getItemRegistry().getItem("core:anvil")).createItemStack())
                .wikiEntry("Test", url)
                .descriptionLines(List.of(Translations.rawComponentLines("core.menu.recipe.hammering.info.description")))
                .build());
        setBackground(Menu.INVISIBLE_BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_recipe_viewer_hammering>").font(NEXO);
    }
}
