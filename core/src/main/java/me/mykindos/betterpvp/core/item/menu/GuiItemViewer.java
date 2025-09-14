package me.mykindos.betterpvp.core.item.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.item.impl.AutoUpdateItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiItemViewer extends AbstractPagedGui<ItemInstance> implements Windowed {

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final ItemFactory itemFactory;
    private boolean customOnly = true;
    private ItemRarity raritySearch = null;
    private String nameSearch = null;
    private CompletableFuture<Void> searchFuture = CompletableFuture.completedFuture(null);

    public GuiItemViewer(ItemFactory itemFactory) {
        super(9, 6, false, new Structure(
                "0XXX0XXX0",
                "0XXX0XXX0",
                "<XXX0XXX>",
                "0XXX0XXX0",
                "0XXX0XXX0",
                "00000000I"
        ).addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', PageBackwardButton.defaultTexture())
                .addIngredient('>', PageForwardButton.defaultTexture())
                .addIngredient('I', InfoTabButton.builder()
                        // todo: wiki entry
                        .wikiEntry("Test", url)
                        .description(Component.text("Most items have a recipe they can be obtained with. The anvil, workbench, smelter and imbuement pedestal all make use of recipes listed here."))
                        .build()));

        setItem(45, new NameSearchButton());
        setItem(46, new CustomOnlyButton());
        setItem(47, new RaritySearchButton());
        setItem(52, new AutoUpdateItem(1, () -> {
            if (!searchFuture.isDone()) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/exclamation_mark_icon"))
                        .displayName(Component.text("Loading...", NamedTextColor.RED))
                        .build();
            }
            return Menu.INVISIBLE_BACKGROUND_ITEM;
        }));

        addPageChangeHandler((previousPage, nextPage) -> {
            for (Player player : findAllCurrentViewers()) {
                new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN).play(player);
            }
        });

        this.itemFactory = itemFactory;
        refresh();
    }

    public void refresh() {
        this.searchFuture = CompletableFuture.supplyAsync(() -> {
            // do heavy work off-thread
            final Map<NamespacedKey, BaseItem> pool = itemFactory.getItemRegistry().getItemsSorted();

            // sort entries by key (case-insensitive, key only)
            List<Map.Entry<NamespacedKey, BaseItem>> entries = new ArrayList<>(pool.entrySet());
            entries.sort(Comparator.comparing(
                    entry -> entry.getKey().getKey(),
                    String.CASE_INSENSITIVE_ORDER
            ));

            List<ItemInstance> result = new ArrayList<>(entries.size());
            for (Map.Entry<NamespacedKey, BaseItem> entry : entries) {
                if (customOnly && entry.getKey().getNamespace().equals("minecraft")) {
                    continue;
                }
                if (nameSearch != null && !UtilFormat.isSimilar(entry.getKey().getKey(), nameSearch, 0.75) && !entry.getKey().getKey().contains(nameSearch)) {
                    continue;
                }

                ItemInstance instance = itemFactory.create(entry.getValue());
                if (raritySearch == null || instance.getRarity() == raritySearch) {
                    result.add(instance);
                }
            }

            return result;
        }).exceptionally(ex -> {
            log.error("Failed to refresh item viewer", ex);
            return null;
        }).thenAccept(result -> {
            if (result != null) {
                setContent(result);

                for (Player player : findAllCurrentViewers()) {
                    new SoundEffect(Sound.ITEM_SPYGLASS_USE).play(player);
                }
            }
        });
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (ItemInstance item : content) {
            page.add(new SlotElement.ItemSlotElement(new ItemButton(item)));

            if (page.size() >= contentSize) {
                pages.add(page);
                page = new ArrayList<>(contentSize);
            }
        }

        if (!page.isEmpty()) {
            pages.add(page);
        }

        this.pages = pages;
        update();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_recipe_viewer>").font(NEXO);
    }

    private class CustomOnlyButton extends AbstractItem {

        @Override
        public ItemProvider getItemProvider() {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/icon/regular/star_icon"))
                    .displayName(Component.text("Custom Items Only", customOnly ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            customOnly = !customOnly;
            refresh();
            notifyWindows();
        }
    }

    private class RaritySearchButton extends AbstractItem {

        private final ItemRarity[] pool;
        private int index = 0;

        private RaritySearchButton() {
            this.pool = new ItemRarity[ItemRarity.values().length + 1];
            this.pool[0] = null;
            System.arraycopy(ItemRarity.values(), 0, pool, 1, ItemRarity.values().length);
        }

        @Override
        public ItemProvider getItemProvider() {
            final ItemView.ItemViewBuilder builder = ItemView.builder();
            builder.material(Material.PAPER);
            builder.itemModel(Key.key("betterpvp", "menu/icon/regular/crown_icon"));

            boolean titled = false;
            for (ItemRarity itemRarity : pool) {
                TextColor color = raritySearch == itemRarity ? TextColor.color(0xFFD700) : NamedTextColor.GRAY;
                String name = itemRarity == null ? "All Rarities" : itemRarity.getName();
                if (!titled) {
                    builder.displayName(Component.text(name, color));
                    titled = true;
                } else {
                    builder.lore(Component.text(name, color));
                }
            }

            return builder.build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType.isLeftClick()) {
                index = (index + 1) % pool.length;
            } else {
                index = (index - 1 + pool.length) % pool.length;
            }
            raritySearch = pool[index];
            refresh();
            notifyWindows();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private class NameSearchButton extends AbstractItem {

        @Override
        public ItemProvider getItemProvider() {
            if (nameSearch == null) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/magnifying_glass_icon"))
                        .displayName(Component.text("Search", NamedTextColor.GRAY))
                        .action(ClickActions.LEFT, Component.text("Search"))
                        .build();
            }

            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/icon/regular/magnifying_glass_icon"))
                    .displayName(Component.text("Search: ", NamedTextColor.GRAY).
                            append(Component.text(nameSearch, NamedTextColor.GOLD)))
                    .action(ClickActions.LEFT, Component.text("Change Search"))
                    .action(ClickActions.RIGHT, Component.text("Clear Search"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType.isLeftClick()) {
                final Dialog dialog = Dialog.create(this::createDialog);
                player.showDialog(dialog);
            } else {
                nameSearch = null;
                refresh();
                notifyWindows();
            }
        }

        private void createDialog(RegistryBuilderFactory<@NotNull Dialog, ? extends DialogRegistryEntry.Builder> factory) {
            final DialogRegistryEntry.Builder builder = factory.empty();
            final TextDialogInput input = DialogInput.text("search", Component.text("Search an item by name")).maxLength(20).build();
            builder.base(DialogBase.builder(Component.text("Search"))
                    .inputs(List.of(input))
                    .build());

            builder.type(DialogType.confirmation(
               ActionButton.builder(Component.text("Search")).action(DialogAction.customClick((response, audience) -> {
                   final String text = response.getText("search");
                   nameSearch = (text == null || text.isBlank()) ? null : text.toLowerCase().replace(" ", "_");
                   refresh();
                   notifyWindows();
               }, ClickCallback.Options.builder().build())).build(),
               ActionButton.builder(Component.text("Cancel")).build()
            ));
        }
    }
}
