package me.mykindos.betterpvp.core.item.component.impl.socketables;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SocketableDescriptionComponent implements ItemComponent, LoreComponent {

    private static final NamespacedKey COMPONENT_KEY = new NamespacedKey("core", "rune-description");
    private final Socketable socketable;

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return COMPONENT_KEY;
    }

    @Override
    public ItemComponent copy() {
        return new SocketableDescriptionComponent(socketable);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        final List<Component> components = new ArrayList<>(socketable.getDescriptionLines());
        components.add(Component.empty());
        components.add(Translations.component("core.socketable.applies-to")
                .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED));
        for (SocketableGroup value : socketable.getGroups()) {
            components.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✔", NamedTextColor.GREEN))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(value.getDisplayComponent().color(NamedTextColor.WHITE)));
        }
        return components;
    }

    @Override
    public int getRenderPriority() {
        return 0;
    }
}
