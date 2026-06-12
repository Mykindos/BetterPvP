package me.mykindos.betterpvp.core.menu.impl;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class GuiSelectColor extends AbstractGui implements Windowed {

    private final Consumer<DyeColor> selection;
    private final Component title;

    public GuiSelectColor(final Component title, final Consumer<DyeColor> selection) {
        super(9, 5);
        this.selection = selection;
        this.title = title;

        // Second row
        this.setItem(10, this.getItem(Material.BROWN_DYE, "core.menu.select-color.button.brown.name", DyeColor.BROWN));
        this.setItem(11, this.getItem(Material.RED_DYE, "core.menu.select-color.button.red.name", DyeColor.RED));
        this.setItem(12, this.getItem(Material.ORANGE_DYE, "core.menu.select-color.button.orange.name", DyeColor.ORANGE));
        this.setItem(13, this.getItem(Material.YELLOW_DYE, "core.menu.select-color.button.yellow.name", DyeColor.YELLOW));
        this.setItem(14, this.getItem(Material.LIME_DYE, "core.menu.select-color.button.lime.name", DyeColor.LIME));
        this.setItem(15, this.getItem(Material.GREEN_DYE, "core.menu.select-color.button.green.name", DyeColor.GREEN));
        this.setItem(16, this.getItem(Material.LIGHT_BLUE_DYE, "core.menu.select-color.button.light-blue.name", DyeColor.LIGHT_BLUE));

        // Third row
        this.setItem(19, this.getItem(Material.LIGHT_GRAY_DYE, "core.menu.select-color.button.light-gray.name", DyeColor.LIGHT_GRAY));
        this.setItem(20, this.getItem(Material.WHITE_DYE, "core.menu.select-color.button.white.name", DyeColor.WHITE));
        this.setItem(21, this.getItem(Material.PINK_DYE, "core.menu.select-color.button.pink.name", DyeColor.PINK));
        this.setItem(22, this.getItem(Material.MAGENTA_DYE, "core.menu.select-color.button.magenta.name", DyeColor.MAGENTA));
        this.setItem(23, this.getItem(Material.PURPLE_DYE, "core.menu.select-color.button.purple.name", DyeColor.PURPLE));
        this.setItem(24, this.getItem(Material.BLUE_DYE, "core.menu.select-color.button.blue.name", DyeColor.BLUE));
        this.setItem(25, this.getItem(Material.CYAN_DYE, "core.menu.select-color.button.cyan.name", DyeColor.CYAN));

        // Fourth row
        this.setItem(30, this.getItem(Material.GRAY_DYE, "core.menu.select-color.button.gray.name", DyeColor.GRAY));
        this.setItem(32, this.getItem(Material.BLACK_DYE, "core.menu.select-color.button.black.name", DyeColor.BLACK));

        this.setBackground(Menu.BACKGROUND_ITEM);
    }

    public GuiSelectColor(final Consumer<DyeColor> selection) {
        this(Translations.component("core.menu.select-color.title"), selection);
    }

    private SimpleItem getItem(final Material material, final String key, final DyeColor color) {
        final ItemBuilder item = new ItemBuilder(material);
        final Component name = Translations.component(key).color(TextColor.color(color.getColor().asRGB())).decoration(TextDecoration.ITALIC, false);
        final AdventureComponentWrapper displayName = new AdventureComponentWrapper(name);
        item.setDisplayName(displayName);
        return new SimpleItem(item, click -> {
            GuiSelectColor.this.selection.accept(color);
            SoundEffect.HIGH_PITCH_PLING.play(click.getPlayer());
        });
    }

    @NotNull
    @Override
    public Component getTitle() {
        return this.title;
    }

}