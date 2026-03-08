package me.mykindos.betterpvp.core.utilities.model.display.bossbar;

import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * Immutable snapshot of what a single boss bar slot should display.
 */
@Getter
public class BossBarData {

    private final Component name;
    /** Clamped to [0, 1]. */
    private final float progress;
    private final BossBar.Overlay overlay;

    public BossBarData(Component name, float progress) {
        this(name, progress, BossBar.Overlay.PROGRESS);
    }

    public BossBarData(Component name, float progress, BossBar.Overlay overlay) {
        this.name = name;
        this.progress = Math.max(0f, Math.min(1f, progress));
        this.overlay = overlay;
    }
}
