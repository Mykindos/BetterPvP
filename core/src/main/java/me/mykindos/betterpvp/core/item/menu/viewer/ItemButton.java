package me.mykindos.betterpvp.core.item.menu.viewer;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.inventory.window.AbstractSingleWindow;
import me.mykindos.betterpvp.core.inventory.window.AbstractWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.pagination.LoreRotationClock;
import me.mykindos.betterpvp.core.item.renderer.LorePages;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.recipe.menu.GuiRecipeViewer;
import me.mykindos.betterpvp.core.recipe.resolver.ExactIngredientParameter;
import me.mykindos.betterpvp.core.recipe.resolver.ExactResultParameter;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@CustomLog
public class ItemButton extends ControlItem<AbstractGui> implements LoreRotationClock.Rotatable {

    private final ItemInstance itemInstance;
    private final LoreRotationClock rotationClock;
    private final CompletableFuture<Result> loadFuture;

    private volatile Result cachedResult;
    // One pre-rendered provider per visible lore page; rotation just swaps between them.
    private volatile List<ItemProvider> pageProviders;
    private volatile int pageIndex;

    private record Result(LinkedList<Recipe<?>> recipes, LinkedList<Recipe<?>> usages) {}

    public ItemButton(ItemInstance item) {
        this.itemInstance = item;

        Core core = JavaPlugin.getPlugin(Core.class);
        RecipeRegistries registries = core.getInjector().getInstance(RecipeRegistries.class);
        this.rotationClock = core.getInjector().getInstance(LoreRotationClock.class);

        CompletableFuture<LinkedList<Recipe<?>>> recipesFuture =
                registries.getResolver().lookup(new ExactResultParameter(item.getBaseItem()));
        CompletableFuture<LinkedList<Recipe<?>>> usagesFuture =
                registries.getResolver().lookup(new ExactIngredientParameter(item.getBaseItem()));

        this.loadFuture = recipesFuture.thenCombine(usagesFuture, Result::new)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Error loading recipes for item {}", itemInstance.getBaseItem().getClass().getSimpleName()).submit();
                        cachedResult = null;
                    } else {
                        cachedResult = res;
                    }
                    // Build the per-page providers and (re)register for rotation on the main thread.
                    Bukkit.getScheduler().runTask(core, this::onLoaded);
                });
    }

    private void onLoaded() {
        final List<ItemProvider> providers = new ArrayList<>();
        if (cachedResult == null) {
            providers.add(localized(ItemView.of(itemInstance.getView().get())
                    .toBuilder()
                    .lore(Translations.component("core.menu.items.error-recipes"))
                    .build()));
        } else {
            for (int page : LorePages.visiblePages(itemInstance)) {
                providers.add(buildProvider(page));
            }
            if (providers.isEmpty()) {
                providers.add(buildProvider(LorePages.mostRelevant(itemInstance)));
            }
        }
        this.pageProviders = providers;
        notifyWindows();
        maybeRegisterRotation();
    }

    private ItemProvider buildProvider(int page) {
        ItemView.ItemViewBuilder builder = ItemView.of(itemInstance.getView().get(null, page)).toBuilder();
        if (cachedResult != null) {
            if (!cachedResult.recipes.isEmpty()) {
                builder.action(ClickActions.LEFT, Translations.component("core.menu.items.button.view-recipes.name"));
            }
            if (!cachedResult.usages.isEmpty()) {
                builder.action(ClickActions.RIGHT, Translations.component("core.menu.items.button.view-usages.name"));
            }
        }
        return localized(builder.build());
    }

    @Override
    public ItemProvider getItemProvider(AbstractGui gui) {
        final List<ItemProvider> providers = pageProviders;
        if (providers == null || providers.isEmpty()) {
            return localized(ItemView.of(itemInstance.getView().get())
                    .toBuilder()
                    .lore(Translations.component(loadFuture.isDone() ? "core.menu.items.error-recipes" : "core.menu.items.loading-recipes"))
                    .build());
        }
        return providers.get(Math.min(pageIndex, providers.size() - 1));
    }

    @Override
    public void rotateTick() {
        final List<ItemProvider> providers = pageProviders;
        if (providers == null || providers.size() <= 1) {
            return;
        }
        pageIndex = (pageIndex + 1) % providers.size();
        notifyWindows();
    }

    @Override
    public void addWindow(AbstractWindow window) {
        super.addWindow(window);
        maybeRegisterRotation();
    }

    @Override
    public void removeWindow(AbstractWindow window) {
        super.removeWindow(window);
        if (getWindows().isEmpty()) {
            rotationClock.unregister(this);
        }
    }

    private void maybeRegisterRotation() {
        final List<ItemProvider> providers = pageProviders;
        if (providers != null && providers.size() > 1 && !getWindows().isEmpty()) {
            rotationClock.register(this);
        }
    }

    /**
     * Wraps an (unresolved) preview view in a provider that resolves its translatable name/lore into the
     * viewer's locale at draw time. The menu framework calls {@link ItemProvider#get(String)} per viewer
     * with that viewer's language, so each player sees the item in their own language. This is needed
     * because item previews are built from {@code getView().get()} with their PDC stripped, so the outgoing
     * packet remapper cannot re-expand them — we localize here instead.
     */
    private ItemProvider localized(ItemView view) {
        final var base = view.get();
        return lang -> Translations.renderItemStack(base, Translations.toLocale(lang));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!loadFuture.isDone() || cachedResult == null) {
            return;
        }

        LinkedList<Recipe<?>> result;
        if (clickType.isLeftClick()) {
            result = cachedResult.recipes;
        } else if (clickType.isRightClick()) {
            result = cachedResult.usages;
        } else {
            return;
        }

        if (result.isEmpty()) {
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

        for (Player viewer : getGui().findAllCurrentViewers()) {
            new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN).play(viewer);
        }
        new GuiRecipeViewer(result, previous).show(player);
    }
}
