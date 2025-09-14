package me.mykindos.betterpvp.core.item.menu;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.menu.GuiRecipeViewer;
import me.mykindos.betterpvp.core.recipe.smelting.Alloy;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipe;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

public class AlloyButton extends ControlItem<Gui> {

    private final Alloy alloy;
    private final int millibuckets;
    private final String millibucketPrefix;
    private final boolean viewOnly;
    private final CompletableFuture<Result> loadFuture;

    private volatile Result cachedResult;
    private volatile ItemProvider cachedProvider;

    private record Result(LinkedList<Recipe<?, ?>> recipes, LinkedList<Recipe<?, ?>> usages) {}

    public AlloyButton(Alloy alloy, int millibuckets, boolean viewOnly, String millibucketPrefix) {
        this.viewOnly = viewOnly;
        this.millibuckets = millibuckets;
        this.alloy = alloy;
        this.millibucketPrefix = millibucketPrefix;

        RecipeRegistries registries = JavaPlugin.getPlugin(Core.class)
                .getInjector().getInstance(RecipeRegistries.class);

        CompletableFuture<LinkedList<Recipe<?, ?>>> recipesFuture =
                registries.getResolver().lookup(recipe -> {
                    if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                        return smeltingRecipe.getPrimaryResult().getPrimaryResult().getAlloyType() == alloy;
                    }
                    return false;
                });

        CompletableFuture<LinkedList<Recipe<?, ?>>> usagesFuture =
                registries.getResolver().lookup(recipe -> {
                    if (recipe instanceof CastingMoldRecipe castingMoldRecipe) {
                        return castingMoldRecipe.getAlloy() == alloy;
                    }
                    return false;
                });

        this.loadFuture = recipesFuture.thenCombine(usagesFuture, Result::new)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        cachedResult = null;
                        cachedProvider = baseBuilder()
                                .lore(Component.text("Error loading recipes"))
                                .build();
                    } else {
                        cachedResult = res;
                        ItemView.ItemViewBuilder builder = baseBuilder();
                        if (!res.recipes.isEmpty()) {
                            builder.action(ClickActions.LEFT, Component.text("View Recipes"));
                        }
                        if (!res.usages.isEmpty()) {
                            builder.action(ClickActions.RIGHT, Component.text("View Usages"));
                        }
                        cachedProvider = builder.build();
                    }
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Core.class), this::notifyWindows);
                });
    }

    private ItemView.ItemViewBuilder baseBuilder() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/sprite/smelter/alloy_indicator/" + alloy.getTextureKey()))
                .displayName(Component.text(alloy.getName(),
                        TextColor.color(alloy.getColor().asRGB()), TextDecoration.BOLD))
                .customModelData(15)
                .lore(Component.text(millibucketPrefix + ":", TextColor.color(214, 214, 214))
                        .appendSpace()
                        .append(Component.text(millibuckets, NamedTextColor.WHITE))
                        .append(Component.text(" mb", NamedTextColor.WHITE)));
    }

    @Override
    public ItemProvider getItemProvider(Gui gui) {
        if (viewOnly) {
            return baseBuilder().build();
        }
        if (!loadFuture.isDone()) {
            return baseBuilder()
                    .lore(Component.text("Loading recipes..."))
                    .build();
        }
        if (cachedProvider != null) {
            return cachedProvider;
        }
        return baseBuilder()
                .lore(Component.text("Error loading recipes"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType,
                            @NotNull Player player,
                            @NotNull InventoryClickEvent event) {
        if (viewOnly || !loadFuture.isDone() || cachedResult == null) {
            SoundEffect.LOW_PITCH_PLING.play(player);
            return;
        }

        LinkedList<Recipe<?, ?>> result;
        if (clickType.isLeftClick()) {
            result = cachedResult.recipes;
        } else if (clickType.isRightClick()) {
            result = cachedResult.usages;
        } else {
            return;
        }

        if (result.isEmpty()) {
            SoundEffect.LOW_PITCH_PLING.play(player);
            return;
        }

        Windowed previous = getGui() instanceof Windowed windowed ? windowed : null;
        for (Window window : getWindows()) {
            if (window instanceof AbstractSingleWindow single
                    && window.getViewer() == player
                    && single.getGui() instanceof Windowed windowed) {
                previous = windowed;
                break;
            }
        }
        new GuiRecipeViewer(result, previous).show(player);
    }
}
