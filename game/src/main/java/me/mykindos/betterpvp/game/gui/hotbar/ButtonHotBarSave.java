package me.mykindos.betterpvp.game.gui.hotbar;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayout;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class ButtonHotBarSave extends AbstractItem {

    private final HotBarLayoutManager manager;
    private final HotBarLayout original;
    private final HotBarLayout updated;
    private final Consumer<Player> onSave;

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Resources.ItemModel.INVISIBLE)
                .hideTooltip(true)
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Only save if the layout has changed, so we don't spam the DB
        if (!original.equals(updated)) {
            original.copy(updated);
            manager.saveLayout(player, updated);
        }
        player.closeInventory();
        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f).play(player);
        onSave.accept(player);
    }
}
