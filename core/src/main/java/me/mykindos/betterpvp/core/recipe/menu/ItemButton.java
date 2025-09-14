package me.mykindos.betterpvp.core.recipe.menu;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.resolver.ExactIngredientParameter;
import me.mykindos.betterpvp.core.recipe.resolver.ExactResultParameter;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

@CustomLog
public class ItemButton extends ControlItem<AbstractGui> {

    private final ItemInstance itemInstance;
    private final CompletableFuture<Result> loadFuture;

    private volatile Result cachedResult;
    private volatile ItemProvider cachedProvider;

    private record Result(LinkedList<Recipe<?, ?>> recipes, LinkedList<Recipe<?, ?>> usages) {}

    public ItemButton(ItemInstance item) {
        this.itemInstance = item;

        RecipeRegistries registries = JavaPlugin.getPlugin(Core.class)
                .getInjector().getInstance(RecipeRegistries.class);

        CompletableFuture<LinkedList<Recipe<?, ?>>> recipesFuture =
                registries.getResolver().lookup(new ExactResultParameter(item.getBaseItem()));
        CompletableFuture<LinkedList<Recipe<?, ?>>> usagesFuture =
                registries.getResolver().lookup(new ExactIngredientParameter(item.getBaseItem()));

        this.loadFuture = recipesFuture.thenCombine(usagesFuture, Result::new)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Error loading recipes for item {}", itemInstance.getBaseItem().getClass().getSimpleName()).submit();
                        cachedResult = null;
                        cachedProvider = ItemView.of(itemInstance.getView().get())
                                .toBuilder()
                                .lore(Component.text("Error loading recipes"))
                                .build();
                    } else {
                        cachedResult = res;
                        ItemView.ItemViewBuilder builder = ItemView.of(itemInstance.getView().get()).toBuilder();
                        if (!res.recipes.isEmpty()) {
                            builder.action(ClickActions.LEFT, Component.text("View Recipes"));
                        }
                        if (!res.usages.isEmpty()) {
                            builder.action(ClickActions.RIGHT, Component.text("View Usages"));
                        }
                        cachedProvider = builder.build();
                    }
                    // update GUI on main thread
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Core.class), this::notifyWindows);
                });
    }

    @Override
    public ItemProvider getItemProvider(AbstractGui gui) {
        if (!loadFuture.isDone()) {
            return ItemView.of(itemInstance.getView().get())
                    .toBuilder()
                    .lore(Component.text("Loading recipes..."))
                    .build();
        }
        if (cachedProvider != null) {
            return cachedProvider;
        }
        return ItemView.of(itemInstance.getView().get())
                .toBuilder()
                .lore(Component.text("Error loading recipes"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!loadFuture.isDone() || cachedResult == null) {
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
