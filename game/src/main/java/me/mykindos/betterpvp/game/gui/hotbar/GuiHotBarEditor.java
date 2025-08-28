package me.mykindos.betterpvp.game.gui.hotbar;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * GUI for editing your {@link HotBarLayout}
 */
public class GuiHotBarEditor extends AbstractGui implements Windowed {

    public GuiHotBarEditor(HotBarLayoutManager manager, ItemFactory itemFactory, HotBarLayout hotBarLayout, Consumer<Player> onSave, Windowed previous) {
        super(9, 2);

        // Clone the layout so that can be edited without affecting the original, until confirmed
        final HotBarLayout clone = new HotBarLayout(hotBarLayout);

        // Set item button
        for (int slot = 0; slot < 9; slot++) {
            setItem(slot, new ButtonHotBarSlot(itemFactory, clone, slot));
        }

        // Cancel button
        final BackButton backButton = new BackButton(previous, Resources.ItemModel.INVISIBLE, null);
        fill(10, 13, backButton, true);

        ButtonHotBarSave saveButton = new ButtonHotBarSave(manager, hotBarLayout, clone, onSave);
        fill(14, 17, saveButton, true);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("space.-8", NamedTextColor.WHITE).font(Resources.Font.SPACE)
                .append(Component.text("<glyph:menu_hotbar_editor>").font(Resources.Font.NEXO))
                .append(Component.translatable("space.8", NamedTextColor.WHITE).font(Resources.Font.SPACE));
    }
}
