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

public class StructuresButton extends ControlItem<GuiCreateWorld> {

    private boolean generateStructures;

    @Override
    public ItemProvider getItemProvider(GuiCreateWorld gui) {
        return ItemView.builder()
                .material(Material.FURNACE)
                .displayName(Component.text("Generate Structures: ", NamedTextColor.GRAY)
                        .append(Component.text(this.generateStructures, this.generateStructures ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .action(ClickActions.ALL, Component.text(this.generateStructures ? "Disable" : "Enable"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        this.generateStructures = !this.generateStructures;
        getGui().getCreator().generateStructures(generateStructures);
        notifyWindows();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
