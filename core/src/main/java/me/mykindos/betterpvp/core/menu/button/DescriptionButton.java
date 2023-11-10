package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.Map;
import java.util.function.Consumer;

public class DescriptionButton extends AbstractItem {

    private final Description description;
    private final ItemProvider itemProvider;

    public DescriptionButton(Description description) {
        this.description = description;

        final ItemProvider icon = description.getIcon();
        final Map<String, Component> properties = description.getProperties();
        if (properties != null) {
            this.itemProvider = ItemView.of(icon.get()).toBuilder()
                    .lore(properties.entrySet().stream().map(entry -> {
                        final TextComponent key = Component.text(entry.getKey() + ": ", NamedTextColor.GRAY);
                        return key.append(entry.getValue());
                    }).toList())
                    .frameLore(true)
                    .build();
        } else {
            this.itemProvider = icon;
        }
    }

    @Override
    public ItemProvider getItemProvider() {
        return itemProvider;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final Consumer<Click> func = description.getClickFunction();
        if (func != null) {
            func.accept(new Click(event));
        }
    }
}
