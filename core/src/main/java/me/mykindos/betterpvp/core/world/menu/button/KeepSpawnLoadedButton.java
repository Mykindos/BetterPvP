package me.mykindos.betterpvp.core.world.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.menu.GuiCreateWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class KeepSpawnLoadedButton extends ControlItem<GuiCreateWorld> {

    private boolean keepSpawnLoaded;

    @Override
    public ItemProvider getItemProvider(GuiCreateWorld gui) {
        return ItemView.builder()
                .material(this.keepSpawnLoaded ? Material.PANDA_SPAWN_EGG : Material.GHAST_SPAWN_EGG)
                .displayName(Component.text("Keep Spawn Loaded: ", NamedTextColor.GRAY)
                        .append(Component.text(this.keepSpawnLoaded, this.keepSpawnLoaded ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .action(ClickActions.ALL, Component.text(this.keepSpawnLoaded ? "Disable" : "Enable"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        this.keepSpawnLoaded = !this.keepSpawnLoaded;
        getGui().getCreator().keepSpawnLoaded(TriState.byBoolean(keepSpawnLoaded));
        notifyWindows();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
