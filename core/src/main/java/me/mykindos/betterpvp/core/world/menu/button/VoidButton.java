package me.mykindos.betterpvp.core.world.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.generator.VoidWorldGenerator;
import me.mykindos.betterpvp.core.world.menu.GuiCreateWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class VoidButton extends ControlItem<GuiCreateWorld> {

    private boolean voidWorld;

    @Override
    public ItemProvider getItemProvider(GuiCreateWorld gui) {
        return ItemView.builder()
                .material(Material.BLACK_CONCRETE)
                .displayName(Component.text("Void World: ", NamedTextColor.GRAY)
                        .append(Component.text(this.voidWorld, this.voidWorld ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .action(ClickActions.ALL, Component.text(this.voidWorld ? "Disable" : "Enable"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        this.voidWorld = !this.voidWorld;
        getGui().getCreator().generator(voidWorld ? new VoidWorldGenerator() : null);
        notifyWindows();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
