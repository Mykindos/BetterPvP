package me.mykindos.betterpvp.core.utilities.model.display.bossbar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;

/**
 * Semantic boss bar color slots, mapping to Adventure's {@link BossBar.Color}.
 *
 * <p>{@link #TRANSPARENT} corresponds to Adventure's {@code PINK}, which renders
 * with a semi-transparent appearance. All other slots keep the name of their underlying color.
 *
 * <p>HUD overlay boss bars (Adventure's {@code WHITE}) are managed separately by
 * {@link BossBarOverlay} and are not part of this enum.
 */
@Getter
@RequiredArgsConstructor
public enum BossBarColor {
    TRANSPARENT(BossBar.Color.PINK),
    BLUE(BossBar.Color.BLUE),
    RED(BossBar.Color.RED),
    GREEN(BossBar.Color.GREEN),
    YELLOW(BossBar.Color.YELLOW),
    PURPLE(BossBar.Color.PURPLE);

    private final BossBar.Color adventureColor;
}
