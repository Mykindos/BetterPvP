package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.site.Whereabouts;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A page of clan members, one item each, for the Barracks' information upgrades. */
class MemberListMenu extends AbstractPagedGui<Item> implements Windowed {

    private final Component title;

    MemberListMenu(@NotNull Component title, @NotNull List<Item> members, @Nullable Windowed previous) {
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
        this.title = title;
        setContent(members);
    }

    /** Where {@code whereabouts} is: its site or world, then its zone, or its block when no zone covers it. */
    static @NotNull Component place(@NotNull Whereabouts whereabouts) {
        final Component zone = whereabouts.getZone();
        return zone != null
                ? Translations.component("clans.camp.upgrade.place.zone", whereabouts.getPlace(), zone)
                : Translations.component("clans.camp.upgrade.place.block", whereabouts.getPlace(),
                Component.text(whereabouts.getX()), Component.text(whereabouts.getY()),
                Component.text(whereabouts.getZ()));
    }

    static @NotNull String name(@NotNull ClanMember member) {
        return Objects.requireNonNullElse(member.getClientName(), member.getUuid().toString());
    }

    static @NotNull Component ago(long millis) {
        return Translations.component("clans.camp.upgrade.place.ago",
                Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(Math.max(0, millis)))));
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
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
}
