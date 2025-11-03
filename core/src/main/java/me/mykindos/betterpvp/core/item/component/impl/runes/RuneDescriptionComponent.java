package me.mykindos.betterpvp.core.item.component.impl.runes;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static me.mykindos.betterpvp.core.utilities.UtilMessage.miniMessage;

@AllArgsConstructor
public class RuneDescriptionComponent implements ItemComponent, LoreComponent {

    private static final NamespacedKey COMPONENT_KEY = new NamespacedKey("core", "rune-description");
    private final Rune rune;

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return COMPONENT_KEY;
    }

    @Override
    public ItemComponent copy() {
        return new RuneDescriptionComponent(rune);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        final List<Component> components = ComponentWrapper.wrapLine(miniMessage.deserialize("<gray>" + rune.getDescription()));
        components.add(Component.empty());
        components.add(Component.text("Applies to:", NamedTextColor.GRAY, TextDecoration.UNDERLINED));
        for (RuneGroup value : RuneGroup.values()) {
            final String displayName = value.getDisplayName();
            final boolean contains = rune.getGroups().contains(value);
            if (contains) {
                components.add(Component.text("[", NamedTextColor.GRAY)
                        .append(Component.text("✔", NamedTextColor.GREEN))
                        .append(Component.text("] ", NamedTextColor.GRAY))
                        .append(Component.text(displayName, NamedTextColor.WHITE)));
            }
        }
        return components;
    }

    @Override
    public int getRenderPriority() {
        return 0;
    }
}
