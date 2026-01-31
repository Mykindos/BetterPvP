package me.mykindos.betterpvp.core.interaction.input;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in interaction input types.
 */
public enum InteractionInputs implements InteractionInput {

    // Click inputs
    RIGHT_CLICK("RIGHT-CLICK"),
    LEFT_CLICK("LEFT-CLICK"),
    SHIFT_RIGHT_CLICK("SHIFT RIGHT-CLICK"),
    SHIFT_LEFT_CLICK("SHIFT LEFT-CLICK"),

    // Hold inputs
    HOLD_RIGHT_CLICK("HOLD RIGHT-CLICK"),
    HOLD("HOLD"),

    // Movement inputs
    JUMP("JUMP"), // todo: implement
    DOUBLE_JUMP("DOUBLE JUMP"), // todo: implement
    SNEAK_START("SNEAK"),
    SNEAK_END("SNEAK RELEASE"),
    SPRINT_START("SPRINT"), // todo: implement

    // Item action inputs
    DROP_ITEM("DROP ITEM"), // todo: implement
    SWAP_HAND(Component.keybind("key.swapOffhand").append(Component.text(" KEY"))),

    // Passive trigger inputs
    DAMAGE_DEALT("DAMAGE DEALT"),
    DAMAGE_TAKEN("DAMAGE TAKEN"),
    PROJECTILE_HIT("PROJECTILE HIT"), // todo: implement
    KILL("KILL"),

    // Passive (no trigger, effect delegated to interaction)
    PASSIVE("PASSIVE"),
    NONE("");

    @Getter
    private final Component displayName;
    private final String name;

    InteractionInputs(@NotNull String displayName) {
        this.displayName = Component.text(displayName);
        this.name = name();
    }

    InteractionInputs(@NotNull Component displayName) {
        this.displayName = displayName;
        this.name = name();
    }

    @Override
    public String getName() {
        return name;
    }
}
