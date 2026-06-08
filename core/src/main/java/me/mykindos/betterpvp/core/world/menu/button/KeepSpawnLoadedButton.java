package me.mykindos.betterpvp.core.world.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.menu.GuiCreateWorld;
import me.mykindos.betterpvp.core.locale.Translations;
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
                .displayName(Translations.component("core.menu.world.create.button.keep-spawn-loaded.name").color(NamedTextColor.GRAY)
                        .append(Component.text(this.keepSpawnLoaded, this.keepSpawnLoaded ? NamedTextColor.GREEN : NamedTextColor.RED)))
                .action(ClickActions.ALL, this.keepSpawnLoaded
                        ? Translations.component("core.menu.world.create.button.disable.action")
                        : Translations.component("core.menu.world.create.button.enable.action"))
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
