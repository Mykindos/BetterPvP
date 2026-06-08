package me.mykindos.betterpvp.core.item.component.impl;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class DescriptionComponent extends AbstractItemComponent implements LoreComponent {

    public static final int FIRST = -Integer.MAX_VALUE;
    public static final int LAST = Integer.MAX_VALUE;

    private final int renderPriority;
    private final Component component;
    /**
     * Translation base key for translatable lore. When set, the lore lines are resolved lazily
     * (per render / per viewer locale) via {@link Translations#componentLines}. Resolving lazily
     * means this works regardless of whether the owning module's translation bundle has been
     * registered yet at item-construction time.
     */
    private final String translationKey;
    private final Component[] translationArgs;

    public DescriptionComponent(int renderPriority, Component description) {
        super("description");
        this.renderPriority = renderPriority;
        this.component = description;
        this.translationKey = null;
        this.translationArgs = null;
    }

    private DescriptionComponent(int renderPriority, String translationKey, Component[] translationArgs) {
        super("description");
        this.renderPriority = renderPriority;
        this.component = null;
        this.translationKey = translationKey;
        this.translationArgs = translationArgs;
    }

    /**
     * Creates a translatable, multi-line description from a translation base key.
     *
     * <p>The lore is sourced (lazily, at render time) via
     * {@link Translations#componentLines(String, ComponentLike...)}, which reads a
     * {@code <baseKey>.lines} count and the per-line {@code <baseKey>.1}, {@code <baseKey>.2} ... keys.
     * Each line is localized per viewer and rendered with the default gray, non-italic lore styling.</p>
     *
     * @param renderPriority the lore render priority (lower renders first)
     * @param baseKey        the translation base key (e.g. {@code core.item.tree-bark.lore})
     * @param args           styled component arguments substituted into the lore lines
     * @return a description component backed by translatable lines
     */
    public static DescriptionComponent translatable(int renderPriority, String baseKey, Component... args) {
        return new DescriptionComponent(renderPriority, baseKey, args);
    }

    @Override
    public DescriptionComponent copy() {
        if (translationKey != null) {
            return new DescriptionComponent(renderPriority, translationKey, translationArgs);
        }
        return new DescriptionComponent(renderPriority, component);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        if (translationKey != null) {
            return Arrays.asList(Translations.componentLines(translationKey, translationArgs));
        }
        final Style style = Style.style().decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY).build();
        final Component normalized = component.applyFallbackStyle(style);
        return ComponentWrapper.wrapLine(normalized, 30);
    }

    @Override
    public int getRenderPriority() {
        return renderPriority;
    }
}
