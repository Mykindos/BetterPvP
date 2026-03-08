package me.mykindos.betterpvp.game.gui.hotbar;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarItem;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@Setter
@CustomLog
public class HotBarEditor extends AbstractGui {
    private static final Map<Integer, Integer> playerInventoryToGuiIndex = getPlayerInventoryToGuiIndex();
    private static Map<Integer, Integer> getPlayerInventoryToGuiIndex() {
        Map<Integer, Integer> backingMap = new HashMap<>();
        //hotbar GUI is 81-89, normal is 0-8
        int guiIndex = 81;
        for (int i = 0; i < 9; i++) {
            backingMap.put(i, guiIndex);
            guiIndex++;
        }
        //the rest of GUI inventory index 54-80, normal is 9-35
        guiIndex = 54;
        for (int i = 9; i < 36; i++) {
            backingMap.put(i, guiIndex);
            guiIndex++;
        }
        return Map.copyOf(backingMap);
    }

    private final HotBarLayoutManager hotBarLayoutManager;
    private final ItemFactory itemFactory;

    private final Role role;
    private int selectedSlot;
    private final SelectedSlotButton selectedSlotButton;
    //guiIndex, button
    private Map<Integer, EmptySlotButton> emptySlotButtonMap = new HashMap<>();
    private final HotBarLayout original;
    private final HotBarLayout inProgress;
    private final Consumer<Player> onSave;

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     */
    public HotBarEditor(Role role, HotBarLayout current, HotBarLayoutManager manager, ItemFactory itemFactory, Windowed previous, Consumer<Player> onSave) {
        super(9, 10);
        this.hotBarLayoutManager = manager;
        this.itemFactory = itemFactory;

        this.role = role;
        this.original = current;
        inProgress = new HotBarLayout(current);
        selectedSlotButton = new SelectedSlotButton();
        this.onSave = onSave;


        final Structure structure = new Structure(
                "xxxxxxxxp",
                "xooooooox",
                "xooooooox",
                "xooooooox",
                "xooooooox",
                "xxxSxBxxR",
                "eeeeeeeee",
                "eeeeeeeee",
                "eeeeeeeee",
                "eeeeeeeee"
                )
                .addIngredient('x', Menu.BACKGROUND_GUI_ITEM)
                .addIngredient('o', Menu.BACKGROUND_GUI_ITEM)
                .addIngredient('p', new TokensButton()) //points
                .addIngredient('S', new SaveHotbarButton()) //save button
                .addIngredient('B', new BackButton(previous))
                .addIngredient('R', new ResetHotbarButton()) //reset button
                .addIngredient('e', new EmptySlotButton());
        applyStructure(structure);


        setSlotNumbers();

        selectedSlot = -1;
        incrementSelectedSlot();
        updateControlItems();
    }

    @Override
    public void updateControlItems() {
        setEmptySlots();
        updateBuyableItems();
        updateHotBarSlots();
        super.updateControlItems();
    }

    public void updateBuyableItems() {
        final HotBarItem[][] rows = {
            {
                HotBarItem.STANDARD_SWORD, HotBarItem.POWER_SWORD, HotBarItem.BOOSTER_SWORD
            },
            {
                HotBarItem.STANDARD_AXE, HotBarItem.POWER_AXE, HotBarItem.BOOSTER_AXE
            },
            {
                HotBarItem.BOW, HotBarItem.ARROWS
            },
            {
                HotBarItem.MUSHROOM_STEW, HotBarItem.ENERGY_APPLE, HotBarItem.PURIFICATION_POTION
            },
        };
        final int firstIndex = 10;
        for (int i = 0; i < rows.length; i++) {
            HotBarItem[] row = rows[i];
            int guiIndex = firstIndex + (9 * i);
            boolean hasInsufficientPointsMarker = false;
            for (int j = 0; j < row.length; j++) {
                HotBarItem hotBarItem = row[j];
                if (!inProgress.canAddItem(hotBarItem) && !hasInsufficientPointsMarker) {
                    setItem(guiIndex, new InsufficientPointsButton());
                    hasInsufficientPointsMarker = true;
                    guiIndex++;
                }
                if (Arrays.asList(hotBarItem.getAllowedRoles()).contains(role)) {
                    setItem(guiIndex, new HotBarItemButton(itemFactory, hotBarItem));
                } else {
                    setItem(guiIndex, Menu.BACKGROUND_GUI_ITEM);
                }

                guiIndex++;
            }
            setItem(guiIndex, Menu.BACKGROUND_GUI_ITEM);
        }
    }

    public void updateHotBarSlots() {
        inProgress.getLayout().forEach((normalIndex, hotbarItem) -> {
            int guiIndex = playerInventoryToGuiIndex.get(normalIndex);
            setItem(guiIndex, new HotBarItemDisplayButton(hotbarItem, normalIndex));
        });
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
        this.updateControlItems();
    }

    /**
     * Increments the selectedSlot to the next clear slot
     */
    public void incrementSelectedSlot() {
        int newNormalIndex = selectedSlot;
        int count = 0;
        do {
            newNormalIndex++;
            if (newNormalIndex > 35) {
                newNormalIndex = 0;
            }
            //prevent stack overflow if entire inventory is filled
            if (count > 36) break;
            count++;
        }
        while (!isEmpty(newNormalIndex));
        this.selectedSlot = newNormalIndex;
        updateControlItems();
    }

    /**
     * Checks if the supplied normalIndex is currently empty
     * @param normalIndex
     * @return
     */
    private boolean isEmpty(int normalIndex) {
        if (normalIndex == -1) return false;
        return inProgress.getSlot(normalIndex).isEmpty();
    }

    private void setEmptySlots() {
        emptySlotButtonMap.forEach(this::setItem);
        if (selectedSlot == -1) return;
        final int currentIndex = playerInventoryToGuiIndex.get(selectedSlot);
        setItem(currentIndex, selectedSlotButton);
    }

    private void setSlotNumbers() {
        playerInventoryToGuiIndex.forEach((normalIndex, guiIndex) -> {
            final EmptySlotButton item = new EmptySlotButton();
            item.setNormalIndex(normalIndex);
            item.setGuiIndex(guiIndex);
            emptySlotButtonMap.put(guiIndex, item);
            setItem(guiIndex, item);
        });
    }

    public Window show(Player player) {
        Window merged = Window.merged()
                .setTitle(original.getBuild().getRole().getName() + " " + original.getBuild().getId())
                .setGui(this)
                .setViewer(player)
                .build(player);
        merged.open();
        return merged;
    }
}
