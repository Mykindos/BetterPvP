package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.inventory.item.Click;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DescriptionButton extends AbstractItem {

    private final Supplier<Description> description;

    public DescriptionButton(Description description) {
        this.description = () -> description;
    }

    public DescriptionButton(Supplier<Description> description) {
        this.description = description;
    }

    @Override
    public ItemProvider getItemProvider() {
        final Description desc = description.get();
        final ItemProvider icon = desc.getIcon();
        final Map<String, Component> properties = desc.getProperties();
        if (properties != null) {
            return ItemView.of(icon.get()).toBuilder()
                    .lore(properties.entrySet().stream().map(entry -> {
                        final TextComponent key = Component.text(entry.getKey() + ": ", NamedTextColor.GRAY);
                        return key.append(entry.getValue());
                    }).toList())
                    .frameLore(true)
                    .build();
        } else {
            return icon;
        }
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final Consumer<Click> func = description.get().getClickFunction();
        if (func != null) {
            func.accept(new Click(event));
        }
    }
}
