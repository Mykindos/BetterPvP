package me.mykindos.betterpvp.core.recipe.menu;

import com.google.inject.Injector;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AutoCycleItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponent;
import me.mykindos.betterpvp.core.item.menu.viewer.AlloyButton;
import me.mykindos.betterpvp.core.item.menu.viewer.ItemButton;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipe;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingResult;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class GuiSmeltingRecipeViewer extends AbstractGui implements Windowed {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public GuiSmeltingRecipeViewer(SmeltingRecipe recipe) {
        super(7, 5);
        final Injector injector = JavaPlugin.getPlugin(Core.class).getInjector();
        ItemFactory itemFactory = injector.getInstance(ItemFactory.class);

        int index = 0;
        int[] contentSlots = { 11, 12, 13, 18, 19, 20 };
        for (RecipeIngredient ingredient : recipe.getIngredients().values()) {
            final BaseItem item = ingredient.getBaseItem();
            final int amount = ingredient.getAmount();
            final ItemInstance instance = itemFactory.createPreview(item);
            instance.getItemStack().setAmount(amount);
            setItem(contentSlots[index++], new ItemButton(instance));
        }

        // Fuel indicator
        List<ItemProvider> indicatorCycles = new ArrayList<>();
        final float temperature = recipe.getMinimumTemperature();
        final TextColor color = new ProgressColor(temperature / 1_000).inverted().getTextColor();
        final Component indicatorName = Translations.component("core.menu.recipe.smelting.temperature.label").color(TextColor.color(214, 214, 214))
                .appendSpace()
                .append(Component.text((int) temperature + " °C", color));
        for (int i = 0; i < 13; i++) {
            indicatorCycles.add(ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/sprite/smelter/fuel_indicator"))
                    .displayName(indicatorName)
                    .customModelData(i)
                    .build());
        }
        setItem(26, new AutoCycleItem(6, indicatorCycles.toArray(new ItemProvider[0])));

        // Fuel items that have AT LEAST 'temperature' burning point
        List<ItemProvider> fuelItems = new ArrayList<>();
        for (BaseItem item : itemFactory.getItemRegistry().getItems().values()) {
            final Optional<FuelComponent> component = item.getComponent(FuelComponent.class);
            if (component.isEmpty()) {
                continue;
            }

            if (component.get().getMaxTemperature() >= temperature) {
                fuelItems.add(itemFactory.createPreview(item).getView());
            }
        }
        setItem(33, new AutoCycleItem(10, fuelItems.toArray(new ItemProvider[0])));

        final SmeltingResult result = recipe.previewResult();
        setItem(15, new AlloyButton(result.getPrimaryResult().getAlloyType(), result.getTotalMillibuckets(), true, Translations.component("core.menu.alloy.prefix.yields")));
        setItem(5, InfoTabButton.builder()
                // todo: wiki entry
                .icon(itemFactory.createPreview(itemFactory.getItemRegistry().getItem("core:smelter")).createItemStack())
                .wikiEntry("Test", url)
                .descriptionLines(List.of(Translations.rawComponentLines("core.menu.recipe.smelting.info.description")))
                .build());
        setBackground(Menu.INVISIBLE_BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_recipe_viewer_smelting>").font(NEXO);
    }
}
