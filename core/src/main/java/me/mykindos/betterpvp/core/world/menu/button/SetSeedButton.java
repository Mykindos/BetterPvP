package me.mykindos.betterpvp.core.world.menu.button;

import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.inventory.window.AnvilWindow;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.menu.GuiCreateWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class SetSeedButton extends ControlItem<GuiCreateWorld> {

    @Override
    public ItemProvider getItemProvider(GuiCreateWorld gui) {
        return ItemView.builder()
                .material(Material.WHEAT_SEEDS)
                .displayName(Component.text("Seed: ", NamedTextColor.GRAY)
                        .append(Component.text(gui.getCreator().seed(), NamedTextColor.DARK_GREEN)))
                .action(ClickActions.ALL, Component.text("Set seed"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        final AtomicReference<String> seed = new AtomicReference<>();
        final SimpleItem item = new SimpleItem(ItemView.builder()
                .displayName(Component.text("Set Seed", NamedTextColor.GREEN, TextDecoration.BOLD))
                .material(Material.GREEN_CONCRETE)
                .build(), click -> {
            if (seed.get() == null || seed.get().trim().isEmpty()) {
                SoundEffect.WRONG_ACTION.play(player);
                return;
            }

            player.closeInventory();
            getGui().getCreator().seed(UtilWorld.parseSeed(seed.get()));
            notifyWindows();
            getGui().show(player);
            SoundEffect.HIGH_PITCH_PLING.play(player);
        });

        AnvilWindow.single()
                .setGui(Gui.of(new Structure("x#p")
                        .addIngredient('x', ItemView.builder()
                                .material(Material.PAPER)
                                .displayName(Component.text("World Seed", NamedTextColor.GRAY))
                                .build())
                        .addIngredient('p', item)))
                .setTitle("Enter Seed")
                .addRenameHandler(seed::set)
                .addCloseHandler(() -> getGui().show(player))
                .setViewer(player)
                .open(player);
    }
}
