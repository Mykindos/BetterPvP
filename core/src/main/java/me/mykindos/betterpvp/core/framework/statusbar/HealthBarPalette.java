package me.mykindos.betterpvp.core.framework.statusbar;

import lombok.Value;
import net.kyori.adventure.text.format.TextColor;

/**
 * The three colours the {@link StatusBar} health fill paints with: the {@code base} pool, the
 * {@code bonus} overflow tail, and the unlit {@code empty} pieces. The numeric readout reuses
 * {@code base}/{@code bonus} so the text matches the bar.
 * <p>
 * The bar uses the default red/yellow/dark palette unless an active {@link HealthBarTint} effect
 * swaps in its own — turning the bar green under Poison, purple-grey under Vulnerability, etc.
 */
@Value
public class HealthBarPalette {

    TextColor base;
    TextColor bonus;
    TextColor empty;
}
