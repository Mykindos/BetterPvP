package me.mykindos.betterpvp.core.menu.impl;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ConfirmationMenu extends AbstractGui implements Windowed {

    private final Consumer<Boolean> callback;

    public ConfirmationMenu(String description, Consumer<Boolean> callback) {
        super(9, 5);
        this.callback = callback;

        // Titles
        List<Component> titleDescription = Menu.getFixedLore(description);
        SimpleItem titleItem = new SimpleItem(ItemView.builder()
                .material(Material.REDSTONE_TORCH)
                .displayName(Component.text("Confirmation", NamedTextColor.GREEN))
                .lore(titleDescription)
                .build());

        // Yes
        SimpleItem yesItem = new SimpleItem(ItemView.builder().material(Material.LIME_CONCRETE)
                .displayName(Component.text("\u2714", NamedTextColor.GREEN))
                .build(), click -> {
            click.getPlayer().closeInventory();
            callback.accept(true);
            SoundEffect.HIGH_PITCH_PLING.play(click.getPlayer());
        });

        // No
        SimpleItem noItem = new SimpleItem(ItemView.builder().material(Material.RED_CONCRETE)
                .displayName(Component.text("\u2718", NamedTextColor.DARK_RED))
                .build(), click -> {
            click.getPlayer().closeInventory();
            callback.accept(false);
            SoundEffect.LOW_PITCH_PLING.play(click.getPlayer());
        });


        Structure structure = new Structure(
                "#########",
                "#>>>#<<<#",
                "#>>>i<<<#",
                "#>>>#<<<#",
                "#########"
        );

        structure.addIngredient('i', titleItem);
        structure.addIngredient('>', yesItem);
        structure.addIngredient('<', noItem);

        applyStructure(structure);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Confirmation");
    }
}
