package me.mykindos.betterpvp.clans.world.camp.upgrade;

import lombok.Value;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.window.AnvilWindow;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * The Item finder's page. A member types a name into an anvil, or shift-clicks an item in their own inventory, and
 * the page lists every item chest holding a match.
 */
public class ItemFinderMenu extends AbstractGui implements Windowed {

    private final ItemFinder finder;
    private final SiteKey key;
    private final @Nullable Windowed previous;

    ItemFinderMenu(@NotNull ItemFinder finder, @NotNull Player viewer, @NotNull SiteKey key,
                   @Nullable Windowed previous, @Nullable Search search) {
        super(9, 6);
        this.finder = finder;
        this.key = key;
        this.previous = previous;

        setItem(2, new SimpleItem(ItemView.builder()
                .material(Material.NAME_TAG)
                .displayName(Translations.component("clans.camp.upgrade.item_finder.by_name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.item_finder.by_name.description")
                        .color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.item_finder.search"))
                .build(), click -> askName(click.getPlayer())));

        final ItemView.ItemViewBuilder summary = ItemView.builder()
                .material(Material.SPYGLASS)
                .displayName(Translations.component("clans.camp.upgrade.item_finder.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.item_finder.by_item").color(NamedTextColor.GRAY));
        if (search != null) {
            summary.lore(Translations.component("clans.camp.upgrade.item_finder.searching", search.getLabel())
                    .color(NamedTextColor.GRAY));
        }
        setItem(6, new SimpleItem(summary.build()));

        if (search != null) {
            final List<ItemFinder.Found> found = finder.find(viewer, key, search.getTest());
            if (found.isEmpty()) {
                setItem(31, new SimpleItem(ItemView.builder()
                        .material(Material.BARRIER)
                        .displayName(Translations.component("clans.camp.upgrade.item_finder.none")
                                .color(NamedTextColor.GRAY))
                        .build()));
            }
            for (int i = 0; i < found.size() && i < 36; i++) {
                setItem(9 + i, new SimpleItem(result(finder.getChests(), found.get(i))));
            }
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull ItemView result(@NotNull StorehouseChests chests, @NotNull ItemFinder.Found found) {
        return ItemView.builder()
                .material(Material.CHEST)
                .displayName(chests.name(found.getChest()).color(NamedTextColor.YELLOW))
                .frameLore(true)
                .lore(StorehouseChests.position(found.getChest()).color(NamedTextColor.DARK_GRAY))
                .lore(Translations.component("clans.camp.upgrade.item_finder.holds",
                        Component.text(found.getCount(), NamedTextColor.WHITE)).color(NamedTextColor.GRAY))
                .build();
    }

    /** Shift-clicking an item in the member's own inventory searches for that item. */
    @Override
    public void handleItemShift(InventoryClickEvent event) {
        event.setCancelled(true);
        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        final ItemStack sample = clicked.clone();
        new ItemFinderMenu(finder, player, key, previous, new Search(sample.effectiveName(), sample::isSimilar))
                .show(player);
    }

    private void askName(@NotNull Player player) {
        final AtomicReference<String> typed = new AtomicReference<>();
        final SimpleItem confirm = new SimpleItem(ItemView.builder()
                .material(Material.GREEN_CONCRETE)
                .displayName(Translations.component("clans.camp.upgrade.item_finder.search")
                        .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .build(), click -> click.getPlayer().closeInventory());
        final ItemView label = ItemView.builder()
                .material(Material.PAPER)
                .displayName(Translations.component("clans.camp.upgrade.item_finder.prompt"))
                .build();

        AnvilWindow.single()
                .setGui(Gui.of(new Structure("x#p")
                        .addIngredient('x', label)
                        .addIngredient('p', confirm)))
                .setTitle(Translations.component("clans.camp.upgrade.item_finder.by_name"))
                .addRenameHandler(typed::set)
                .addCloseHandler(() -> {
                    final String query = typed.get() == null ? "" : typed.get().trim();
                    final Search search = query.isEmpty() ? null
                            : new Search(Component.text(query), ItemFinder.named(query));
                    new ItemFinderMenu(finder, player, key, previous, search).showAfterClose(player);
                })
                .setViewer(player)
                .open(player);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.item_finder.name");
    }

    /** What is being searched for: how it reads, and what it matches. */
    @Value
    static class Search {
        Component label;
        Predicate<ItemStack> test;
    }
}
