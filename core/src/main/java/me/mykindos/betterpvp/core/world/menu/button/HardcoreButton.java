package me.mykindos.betterpvp.core.world.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.menu.GuiCreateWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class HardcoreButton extends ControlItem<GuiCreateWorld> {

    private boolean hardcore;

    @Override
    public ItemProvider getItemProvider(GuiCreateWorld gui) {
        return ItemView.builder()
                .material(hardcore ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
                .displayName(Component.text("Hardcore: ", NamedTextColor.GRAY)
                        .append(Component.text(this.hardcore, this.hardcore ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .action(ClickActions.ALL, Component.text(this.hardcore ? "Disable" : "Enable"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        this.hardcore = !this.hardcore;
        getGui().getCreator().hardcore(hardcore);
        notifyWindows();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
