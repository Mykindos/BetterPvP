package me.mykindos.betterpvp.clans.world.discovery;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * The wheel position drawn as a bar, read the way Minecraft's locator bar is: everything grey but the one lit
 * character, whose position along the bar <em>is</em> the reading.
 * <p>
 * It tracks the rudder rather than the hull's rate of turn, so the mark moves the instant a control is pressed instead
 * of once the ship has begun to answer. That lag is real and the crew should feel it, but a gauge that lags too is a
 * gauge that reads as broken.
 */
@UtilityClass
public class RudderGauge {

    /** Odd on purpose: an even bar has no middle character, so a centred rudder could not be drawn as centred. */
    public static final int WIDTH = 17;

    /**
     * Which character is lit for {@code rudder}. Hard to port is {@code 0}, centre is the middle character, hard to
     * starboard is the last.
     */
    public static int greenIndex(double rudder) {
        return (int) Math.round((Math.clamp(rudder, -1.0, 1.0) + 1.0) / 2.0 * (WIDTH - 1));
    }

    public static @NotNull Component bar(double rudder) {
        final int lit = greenIndex(rudder);
        final TextComponent.Builder gauge = Component.text();
        for (int index = 0; index < WIDTH; index++) {
            gauge.append(Component.text('|', index == lit ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
        }
        return gauge.build();
    }
}
