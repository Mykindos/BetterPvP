package me.mykindos.betterpvp.core.recipe.menu;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackTabButton;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.resolver.ExactResultParameter;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class GuiCraftingRecipeViewer extends AbstractGui implements Windowed {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final CraftingRecipeRegistry registry;
    private final ItemFactory itemFactory;
    private CraftingRecipe currentRecipe;

    public GuiCraftingRecipeViewer(Collection<CraftingRecipe> recipes, @Nullable Runnable onBack) {
        super(9, 6);
        Preconditions.checkState(!recipes.isEmpty(), "Cannot view an empty recipe");
        final Injector injectory = JavaPlugin.getPlugin(Core.class).getInjector();
        this.registry = injectory.getInstance(CraftingRecipeRegistry.class);
        this.itemFactory = injectory.getInstance(ItemFactory.class);
        this.currentRecipe = recipes.iterator().next();

        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                int index = (y - 1) * 3 + x - 1;
                setItem(x, y, new MatrixSlot(index));
            }
        }

        setItem(24, new ResultButton());
        if (recipes.size() > 1) {
            setItem(26, new CycleButton(recipes));
        }
        setItem(42, new BlueprintButton());
        setItem(52, new BackTabButton(player -> {
            if (onBack != null) {
                onBack.run();
            } else {
                player.closeInventory();
            }
        }));
        setItem(53, InfoTabButton.builder()
                // todo: wiki entry
                .wikiEntry("Test", url)
                .description(Component.text("View crafting recipes. Click on an ingredient to look at its crafting recipes."))
                .build());
        setBackground(Menu.INVISIBLE_BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_crafting_recipe_viewer>").font(NEXO);
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);
        new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f).play(player);
        return window;
    }

    private class CycleButton extends ControlItem<GuiCraftingRecipeViewer> {

        private final Iterator<CraftingRecipe> iterator;

        private CycleButton(Collection<CraftingRecipe> recipes) {
            this.iterator = Iterators.cycle(recipes);
        }

        @Override
        public ItemProvider getItemProvider(GuiCraftingRecipeViewer gui) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/icon/shadowed/arrow_right_icon"))
                    .displayName(Component.text("Next Recipe", NamedTextColor.WHITE))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            currentRecipe = iterator.next();
            updateControlItems();
            new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN, 1.2f, 1f).play(player);
        }
    }

    private class ResultButton extends ControlItem<GuiCraftingRecipeViewer> {

        @Override
        public ItemProvider getItemProvider(GuiCraftingRecipeViewer gui) {
            return ItemView.of(currentRecipe.createPrimaryResult().createItemStack());
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            // ignore
        }
    }

    private class BlueprintButton extends ControlItem<GuiCraftingRecipeViewer> {

        @Override
        public ItemProvider getItemProvider(GuiCraftingRecipeViewer gui) {
            if (currentRecipe != null && currentRecipe.needsBlueprint()) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/shadowed/check_icon"))
                        .displayName(Component.text("Requires Blueprint", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .build();
            } else {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/shadowed/cross_icon"))
                        .displayName(Component.text("No Blueprint Required", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                        .build();
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            // ignore
        }
    }

    @AllArgsConstructor
    private class MatrixSlot extends ControlItem<GuiCraftingRecipeViewer> {

        private final int index;

        @Override
        public ItemProvider getItemProvider(GuiCraftingRecipeViewer gui) {
            final RecipeIngredient ingredient = currentRecipe.getIngredients().get(index);
            if (ingredient == null) {
                return Menu.INVISIBLE_BACKGROUND_ITEM;
            }

            final ItemStack itemStack = itemFactory.create(ingredient.getBaseItem()).getView().get();
            itemStack.setAmount(ingredient.getAmount());
            final ItemView.ItemViewBuilder builder = ItemView.of(itemStack).toBuilder();

            final LinkedList<CraftingRecipe> result = registry.getResolver()
                    .lookup(new ExactResultParameter(ingredient.getBaseItem()));
            if (!result.isEmpty()) {
                builder.action(ClickActions.ALL, Component.text("View Recipes", NamedTextColor.GREEN));
            }

            return builder.build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            final RecipeIngredient ingredient = currentRecipe.getIngredients().get(index);
            if (ingredient == null) {
                return;
            }

            final LinkedList<CraftingRecipe> result = registry.getResolver()
                    .lookup(new ExactResultParameter(ingredient.getBaseItem()));
            if (result.isEmpty()) {
                return;
            }
            new GuiCraftingRecipeViewer(result, () -> show(player)).show(player);
        }
    }
}
