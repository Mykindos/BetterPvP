package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.ClanExperience;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.clans.clans.menus.buttons.PerkMilestoneButton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated clan perk roadmap.
 *
 * <p>Layout (9 × 5 = 45 slots):
 * <pre>
 * Row 0 (header):  # # # # P # # # #   (P = XP progress item)
 * Row 1 (content): # x x x x x x x #
 * Row 2 (content): # x x x x x x x #
 * Row 3 (content): # x x x x x x x #
 * Row 4 (nav):     # # # < - > # # #
 * </pre>
 * 7 perk items per content row × 3 content rows = 21 perks per page.
 */
public class PerkMenu extends AbstractPagedGui<Item> implements Windowed {

    private final @NotNull Clan clan;

    public PerkMenu(@NotNull Clan clan, @Nullable Windowed previous) {
        super(9, 5, false, new Structure(
                "# # # # P # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('P', buildProgressItem(clan))
                .addIngredient('<', PageBackwardButton.defaultTexture().withDisabledInvisible(true))
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', PageForwardButton.defaultTexture().withDisabledInvisible(true)));
        this.clan = clan;
        setContent(buildPerkItems(clan));
    }

    private static Item buildProgressItem(Clan clan) {
        long level = clan.getExperience().getLevel();
        double xpIn = ClanExperience.xpInCurrentLevel(level, clan.getExperience().getXp());
        double xpNeeded = ClanExperience.xpRequiredForNextLevel(level);
        float progress = (xpNeeded > 0) ? (float) Math.min(1.0, xpIn / xpNeeded) : 1f;

        TextComponent progressBar = ProgressBar.withLength(progress, 20)
                .withCharacter(' ')
                .build()
                .decoration(TextDecoration.STRIKETHROUGH, true);

        Component progressLine = Component.text(level, NamedTextColor.YELLOW)
                .appendSpace()
                .append(progressBar)
                .appendSpace()
                .append(Component.text(level + 1, NamedTextColor.YELLOW))
                .appendSpace()
                .append(Component.text(String.format("(%d%%)", (int) (progress * 100)),
                        NamedTextColor.GRAY));

        return ItemView.builder()
                .material(Material.EXPERIENCE_BOTTLE)
                .displayName(Component.text("Clan Level " + level, NamedTextColor.AQUA))
                .lore(progressLine)
                .lore(Component.empty())
                .lore(Component.text("Progress: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                String.format("%,.1f / %,.1f XP", xpIn, xpNeeded),
                                NamedTextColor.YELLOW)))
                .frameLore(true)
                .build()
                .toSimpleItem();
    }

    private List<Item> buildPerkItems(Clan clan) {
        long level = clan.getExperience().getLevel();
        return ClanPerkManager.getInstance().getPerksSortedByLevel().stream()
                .map(perk -> (Item) new PerkMilestoneButton(perk, level))
                .toList();
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;
        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (Item item : content) {
            page.add(new SlotElement.ItemSlotElement(item));
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

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Clan Perks — Level " + clan.getExperience().getLevel(), NamedTextColor.AQUA);
    }

}
