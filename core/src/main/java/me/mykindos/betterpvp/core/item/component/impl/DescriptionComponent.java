package me.mykindos.betterpvp.core.item.component.impl;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class DescriptionComponent extends AbstractItemComponent implements LoreComponent {

    public static final int FIRST = -Integer.MAX_VALUE;
    public static final int LAST = Integer.MAX_VALUE;

    private final int renderPriority;
    private final Component component;

    public DescriptionComponent(int renderPriority, Component description) {
        super("description");
        this.renderPriority = renderPriority;
        this.component = description;
    }

    @Override
    public DescriptionComponent copy() {
        return new DescriptionComponent(renderPriority, component);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        final Style style = Style.style().decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY).build();
        final Component normalized = component.applyFallbackStyle(style);
        return ComponentWrapper.wrapLine(normalized, 30);
    }

    @Override
    public int getRenderPriority() {
        return renderPriority;
    }
}
