package me.mykindos.betterpvp.core.utilities.model.item;

import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ClickActions implements ClickAction {

    ALL(Component.empty()
            .append(Component.translatable("space.-4").font(Resources.Font.SPACE))
            .append(Component.text("\uE073", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16"))),
            ClickType.values()),

    LEFT(Component.empty()
            .append(Component.translatable("space.-4").font(Resources.Font.SPACE))
            .append(Component.text("\uE074", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16"))),
            ClickType.LEFT),

    RIGHT(Component.empty()
            .append(Component.translatable("space.-4").font(Resources.Font.SPACE))
            .append(Component.text("\uE075", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16"))),
            ClickType.RIGHT),

    SHIFT(Component.empty()
            .append(Component.translatable("space.-3").font(Resources.Font.SPACE))
            .append(Component.text("\uE084", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16"))),
            ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT),

    LEFT_SHIFT(Component.empty()
            .append(Component.translatable("space.-3").font(Resources.Font.SPACE))
            .append(Component.text("\uE084", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16")))
            .appendSpace()
            .append(Component.translatable("space.-2").font(Resources.Font.SPACE))
            .append(Component.text("+", NamedTextColor.GRAY))
            .appendSpace()
            .append(Component.translatable("space.-4").font(Resources.Font.SPACE))
            .append(Component.text("\uE074", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16"))),
            ClickType.SHIFT_LEFT),

    RIGHT_SHIFT(Component.empty()
            .append(Component.translatable("space.-3").font(Resources.Font.SPACE))
            .append(Component.text("\uE084", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16")))
            .appendSpace()
            .append(Component.translatable("space.-2").font(Resources.Font.SPACE))
            .append(Component.text("+", NamedTextColor.GRAY))
            .appendSpace()
            .append(Component.translatable("space.-4").font(Resources.Font.SPACE))
            .append(Component.text("\uE075", NamedTextColor.WHITE).font(Key.key("betterpvp", "glyph_16"))),
            ClickType.SHIFT_RIGHT);

    @Getter
    private final Component component;
    private final @NotNull List<ClickType> acceptedClickTypes;

    ClickActions(@NotNull Component component, @NotNull ClickType... acceptedClickTypes) {
        this.component = component;
        this.acceptedClickTypes = List.of(acceptedClickTypes);
    }

    @Override
    public boolean accepts(@NotNull ClickType clickType) {
        return acceptedClickTypes.contains(clickType);
    }
}
