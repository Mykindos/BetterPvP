package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class GuiBlueprintViewer extends AbstractPagedGui<ItemInstance> implements Windowed {

    private final SmartBlockInstance blockInstance;
    private final Workbench workbench;

    protected GuiBlueprintViewer(SmartBlockInstance blockInstance, Workbench workbench) {
        super(9, 6, false, new Structure(
                "000000000",
                "0XXXXXXX0",
                "0XXXXXXX0",
                "0XXXXXXX0",
                "000000000",
                "000<0>000")
                .addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new PreviousButton(() -> ItemView.builder().material(Material.PAPER).itemModel(Resources.ItemModel.INVISIBLE).build()))
                .addIngredient('>', new ForwardButton(() -> ItemView.builder().material(Material.PAPER).itemModel(Resources.ItemModel.INVISIBLE).build())
                ));
        this.blockInstance = blockInstance;
        this.workbench = workbench;
        refresh();
    }

    private void refresh() {
        setContent(((WorkbenchData) Objects.requireNonNull(blockInstance.getData())).getContent());
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (ItemInstance item : content) {
            page.add(new SlotElement.ItemSlotElement(new BlueprintButton(item)));

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
        return Component.text("<shift:-48><glyph:menu_blueprint_viewer>").font(NEXO);
    }

    private class BlueprintButton extends AbstractItem {

        private final ItemInstance itemInstance;

        private BlueprintButton(ItemInstance itemInstance) {
            Preconditions.checkArgument(itemInstance.getBaseItem() instanceof BlueprintItem,
                    "Item instance must be a BlueprintItem, got: " + itemInstance.getBaseItem().getClass().getSimpleName());
            this.itemInstance = itemInstance;
        }

        @Override
        public ItemProvider getItemProvider() {
            return ItemView.of(itemInstance.getView().get()).toBuilder()
                    .action(ClickActions.SHIFT, Component.text("Remove"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType != ClickType.SHIFT_LEFT && clickType != ClickType.SHIFT_RIGHT) {
                return; // Only handle shift clicks
            }

            final ItemStack currentItem = itemInstance.createItemStack();
            if (currentItem.getType() == Material.AIR) {
                return; // No item to remove
            }

            if (!UtilItem.fits(player, currentItem, currentItem.getAmount())) {
                SoundEffect.WRONG_ACTION.play(player);
                return; // Not enough inventory space
            }

            final SmartBlockData<WorkbenchData> blockData = blockInstance.getBlockData();
            blockData.update(storage -> {
                final List<ItemInstance> content = storage.getContent();
                boolean removed = false;

                final Iterator<ItemInstance> iterator = content.iterator();
                while (iterator.hasNext()) {
                    ItemStack item = iterator.next().createItemStack();
                    if (item.isSimilar(currentItem) && item.getAmount() == currentItem.getAmount()) {
                        iterator.remove(); // Remove the item from the storage
                        removed = true;
                        break;
                    }
                }

                if (!removed) {
                    return; // Item was not found in the workbench storage, will not refund
                }

                // Refund
                UtilItem.insert(player, currentItem);
                new SoundEffect(Sound.UI_LOOM_SELECT_PATTERN, 1.1f).play(player);
            });

            refresh();
        }
    }
}
