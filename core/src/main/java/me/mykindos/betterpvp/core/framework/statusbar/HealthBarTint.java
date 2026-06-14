package me.mykindos.betterpvp.core.framework.statusbar;

/**
 * Opt-in capability an effect type implements to recolour the {@link StatusBar} health fill while it
 * is active. The status bar scans the active tinting effects on the viewer and the one with the
 * highest {@link #tintPriority()} wins, so a more urgent condition can override a milder one.
 */
public interface HealthBarTint {

    /** The palette to paint the health bar with while this effect is active. */
    HealthBarPalette healthBarTint();

    /** Higher wins when several tinting effects are active at once. */
    default int tintPriority() {
        return 0;
    }
}
