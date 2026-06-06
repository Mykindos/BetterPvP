package me.mykindos.betterpvp.core.utilities.model.item;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.INPUT;
import static me.mykindos.betterpvp.core.utilities.Resources.Font.SPACE;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public enum ClickActions implements ClickAction {

    ALL(empty()
            .append(text("\uE04C", WHITE).font(INPUT)),
            ClickType.values()),

    LEFT(empty()
            .append(text("\uE04D", WHITE).font(INPUT)),
            ClickType.LEFT),

    RIGHT(empty()
            .append(text("\uE04E", WHITE).font(INPUT)),
            ClickType.RIGHT),

    SHIFT(empty()
            .append(text("\uE0FF", WHITE).font(INPUT))
            .append(translatable("space.-1").font(SPACE))
            .append(text("\uE100", WHITE).font(INPUT)),
            ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT),

    LEFT_SHIFT(empty()
            .append(text("\uE0FF", WHITE).font(INPUT))
            .append(translatable("space.-1").font(SPACE))
            .append(text("\uE100", WHITE).font(INPUT))
            .append(translatable("space.2").font(SPACE))
            .append(text("+", NamedTextColor.GRAY))
            .append(translatable("space.2").font(SPACE))
            .append(text("\uE04D", WHITE).font(INPUT)),
            ClickType.SHIFT_LEFT),

    RIGHT_SHIFT(empty()
            .append(text("\uE0FF", WHITE).font(INPUT))
            .append(translatable("space.-1").font(SPACE))
            .append(text("\uE100", WHITE).font(INPUT))
            .append(translatable("space.2").font(SPACE))
            .append(text("+", NamedTextColor.GRAY))
            .append(translatable("space.2").font(SPACE))
            .append(text("\uE04E", WHITE).font(INPUT)),
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
