package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** The camp ledger's page: every kept entry, newest first, with who did it and how long ago. */
public class CampLedgerMenu extends AbstractPagedGui<Item> implements Windowed {

    CampLedgerMenu(@NotNull CampLedger ledger, @NotNull SiteKey key, @Nullable Windowed previous) {
        super(9, 6, false, new Structure(
                "# # # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PageBackwardButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new PageForwardButton()));

        final long now = System.currentTimeMillis();
        final List<LedgerEntry> entries = ledger.newestFirst(key);
        final List<Item> items = new ArrayList<>();
        for (LedgerEntry entry : entries) {
            items.add(new SimpleItem(ItemView.builder()
                    .material(icon(entry.getKind()))
                    .displayName(ledger.describe(entry).color(NamedTextColor.YELLOW))
                    .lore(Translations.component("clans.camp.upgrade.camp_ledger.by",
                            Component.text(entry.getMemberName() == null ? "?" : entry.getMemberName(),
                                    NamedTextColor.WHITE)).color(NamedTextColor.GRAY))
                    .lore(Translations.component("clans.camp.upgrade.camp_ledger.ago",
                            Component.text(UtilTime.humanReadableFormat(
                                    Duration.ofMillis(Math.max(1000, now - entry.getAt()))), NamedTextColor.WHITE))
                            .color(NamedTextColor.GRAY))
                    .build()));
        }
        if (items.isEmpty()) {
            items.add(new SimpleItem(ItemView.builder()
                    .material(Material.PAPER)
                    .displayName(Translations.component("clans.camp.upgrade.camp_ledger.empty")
                            .color(NamedTextColor.GRAY))
                    .build()));
        }
        setContent(items);
    }

    private static @NotNull Material icon(@NotNull LedgerEntry.Kind kind) {
        return switch (kind) {
            case DEPOSIT -> Material.CHEST;
            case CLAIM -> Material.CRAFTING_TABLE;
            case HIRE -> Material.PLAYER_HEAD;
            case WAGES -> Material.GOLD_INGOT;
        };
    }

    @Override
    public void bake() {
        final int size = getContentListSlots().length;
        final List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(size);
        for (Item item : content) {
            page.add(new SlotElement.ItemSlotElement(item));
            if (page.size() >= size) {
                pages.add(page);
                page = new ArrayList<>(size);
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
        return Translations.component("clans.camp.upgrade.camp_ledger.name");
    }
}
