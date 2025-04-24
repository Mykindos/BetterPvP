package me.mykindos.betterpvp.core.menu.impl;

import me.mykindos.betterpvp.core.inventory.gui.AbstractScrollGui;
import me.mykindos.betterpvp.core.inventory.gui.ScrollGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ScrollItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GuiSelectOne extends AbstractScrollGui<Item> implements Windowed.Textured {

    public GuiSelectOne(List<Item> pool) {
        super(9, 1, false, new Structure("<.......>")
                .addIngredient('<', new ScrollLeftItem())
                .addIngredient('>', new ScrollRightItem())
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_VERTICAL));

        // Because its column
        // InvUI doesnt calculate the width correctly (and, therefore, the amount)
        setLineAmount(7);
        setLineLength(1);
        // end

        setContent(pool);
    }

    @Override
    public void bake() {
        ArrayList<SlotElement> elements = new ArrayList<>(content.size());
        for (Item item : content) {
            elements.add(new SlotElement.ItemSlotElement(item));
        }

        this.elements = elements;
        update();
    }

    @Override
    public char getMappedTexture() {
        return Resources.MenuFontCharacter.SELECT_ONE;
    }

    private static class ScrollLeftItem extends ScrollItem {
        public ScrollLeftItem() {
            super(-1);
        }

        @Override
        public ItemProvider getItemProvider(ScrollGui<?> gui) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .hideTooltip(true)
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            int currentLine = getGui().getCurrentLine();
            super.handleClick(clickType, player, event);
            if (currentLine != getGui().getCurrentLine()) {
                new SoundEffect(Sound.UI_BUTTON_CLICK, 1.2f, 1.0f).play(player);
            }
        }
    }

    private static class ScrollRightItem extends ScrollItem {
        public ScrollRightItem() {
            super(1);
        }

        @Override
        public ItemProvider getItemProvider(ScrollGui<?> gui) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .hideTooltip(true)
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            int currentLine = getGui().getCurrentLine();
            super.handleClick(clickType, player, event);
            if (currentLine != getGui().getCurrentLine()) {
                new SoundEffect(Sound.UI_BUTTON_CLICK, 1.2f, 1.0f).play(player);
            }
        }
    }

}
