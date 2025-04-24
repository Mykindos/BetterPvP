package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * GUI for editing your {@link HotBarLayout}
 */
public class GuiHotBarEditor extends AbstractGui implements Windowed.Textured {

    public GuiHotBarEditor(HotBarLayoutManager manager, ItemHandler itemHandler, HotBarLayout hotBarLayout, Consumer<Player> onSave, Windowed previous) {
        super(9, 2);

        // Clone the layout so that can be edited without affecting the original, until confirmed
        final HotBarLayout clone = new HotBarLayout(hotBarLayout);

        // Set item button
        for (int slot = 0; slot < 9; slot++) {
            setItem(slot, new ButtonHotBarSlot(itemHandler, clone, slot));
        }

        // Cancel button
        final BackButton backButton = new BackButton(previous, Resources.ItemModel.INVISIBLE, null);
        fill(10, 13, backButton, true);

        ButtonHotBarSave saveButton = new ButtonHotBarSave(manager, hotBarLayout, clone, onSave);
        fill(14, 17, saveButton, true);
    }

    @Override
    public char getMappedTexture() {
        return Resources.MenuFontCharacter.HOT_BAR_LAYOUT;
    }
}
