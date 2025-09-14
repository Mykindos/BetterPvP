package me.mykindos.betterpvp.core.item.component.impl.runes;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
        return ComponentWrapper.wrapLine(Component.text(rune.getDescription(), NamedTextColor.GRAY));
    }

    @Override
    public int getRenderPriority() {
        return 0;
    }
}
