package me.mykindos.betterpvp.core.interaction.input;

import lombok.Getter;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in interaction input types. Display names are localized via {@code core.interaction.input.<key>}
 * translation keys (resolved per-viewer when rendered in item lore / messages).
 */
public enum InteractionInputs implements InteractionInput {

    // Click inputs
    RIGHT_CLICK("right-click"),
    LEFT_CLICK("left-click"),
    SHIFT_RIGHT_CLICK("shift-right-click"),
    SHIFT_LEFT_CLICK("shift-left-click"),

    // Inventory
    INVENTORY_LEFT_CLICK("left-click"),
    INVENTORY_RIGHT_CLICK("right-click"),

    // Hold inputs
    HOLD_RIGHT_CLICK("hold-right-click"),
    HOLD("hold"),

    // Movement inputs
    JUMP("jump"), // todo: implement
    DOUBLE_JUMP("double-jump"), // todo: implement
    SNEAK_START("sneak"),
    SNEAK_END("sneak-release"),
    SPRINT_START("sprint"), // todo: implement

    // Item action inputs
    DROP_ITEM("drop-item"), // todo: implement
    SWAP_HAND(Component.keybind("key.swapOffhand").appendSpace()
            .append(Translations.component("core.interaction.input.key"))),
    THROW("throw"),

    // Passive trigger inputs (allow multiple roots)
    DAMAGE_DEALT("damage-dealt", true),
    DAMAGE_TAKEN("damage-taken", true),
    PROJECTILE_HIT("projectile-hit", true), // todo: implement
    KILL("kill", true),
    BLOCK_BREAK("block-break", true),

    // Passive (no trigger, effect delegated to interaction)
    PASSIVE("passive", true),
    NONE("");

    @Getter
    private final Component displayName;
    private final String name;
    private final boolean allowsMultipleRoots;

    InteractionInputs(@NotNull String translationKey) {
        this(translationKey, false);
    }

    InteractionInputs(@NotNull String translationKey, boolean allowsMultipleRoots) {
        this.displayName = translationKey.isEmpty()
                ? Component.empty()
                : Translations.component("core.interaction.input." + translationKey);
        this.name = name();
        this.allowsMultipleRoots = allowsMultipleRoots;
    }

    InteractionInputs(@NotNull Component displayName) {
        this(displayName, false);
    }

    InteractionInputs(@NotNull Component displayName, boolean allowsMultipleRoots) {
        this.displayName = displayName;
        this.name = name();
        this.allowsMultipleRoots = allowsMultipleRoots;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean allowsMultipleRoots() {
        return allowsMultipleRoots;
    }
}
