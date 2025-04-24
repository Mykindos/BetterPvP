package me.mykindos.betterpvp.core.menu.button;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class BackButton extends FlashingButton<Gui> {

    private final Windowed previousMenu;
    private Key itemModel;
    private Runnable onBack;

    public BackButton(Windowed previousMenu, Key itemModel, Runnable onBack) {
        this.previousMenu = previousMenu;
        this.itemModel = itemModel;
        this.onBack = onBack;
    }

    public BackButton(Windowed previousMenu, Runnable onBack) {
        this(previousMenu, null, onBack);
    }

    public BackButton(Windowed previousMenu) {
        this.previousMenu = previousMenu;
    }

    @Override
    public ItemProvider getItemProvider(Gui gui) {
        final Component standardComponent = Component.text(previousMenu == null ? "Close" : "Back", NamedTextColor.RED);
        final Component flashComponent = Component.empty().append(Component.text("Click Me!", NamedTextColor.GREEN)).appendSpace().append(standardComponent);

        final ItemView.ItemViewBuilder builder = ItemView.builder();
        builder.material(Material.BARRIER);
        if (itemModel != null) {
            builder.itemModel(itemModel);
        }

        return builder
                .displayName(this.isFlashing() ? flashComponent : standardComponent)
                .glow(this.isFlash())
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1.4f, 0.6f).play(player);
        if (previousMenu == null) {
            player.closeInventory();
            return;
        }

        previousMenu.show(player);
        if (onBack != null) {
            onBack.run();
        }
    }
}
