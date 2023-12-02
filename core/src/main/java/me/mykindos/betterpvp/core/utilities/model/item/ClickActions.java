package me.mykindos.betterpvp.core.utilities.model.item;

import lombok.Getter;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ClickActions implements ClickAction {

    ALL("Click", ClickType.values()),
    LEFT("Left-Click", ClickType.LEFT),
    RIGHT("Right-Click", ClickType.RIGHT),
    SHIFT("Shift-Click", ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT),
    LEFT_SHIFT("Shift-Left-Click", ClickType.SHIFT_LEFT),
    RIGHT_SHIFT("Shift-Right-Click", ClickType.SHIFT_RIGHT);

    @Getter
    private final String name;
    private final @NotNull List<ClickType> acceptedClickTypes;

    ClickActions(@NotNull String name, @NotNull ClickType... acceptedClickTypes) {
        this.name = name;
        this.acceptedClickTypes = List.of(acceptedClickTypes);
    }

    @Override
    public boolean accepts(@NotNull ClickType clickType) {
        return acceptedClickTypes.contains(clickType);
    }
}
