package me.mykindos.betterpvp.clans.world.discovery.sighting;

import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The glow each island template is sighted in, so a crew can tell what they are steering for from the horizon.
 * <p>
 * One place, because it grows with the island list: a template with no colour of its own is sighted in plain landfall
 * gold rather than going unmarked.
 */
public final class SightingPalette {

    private static final Map<String, Color> BY_TEMPLATE = Map.of(
            "mining", Color.fromRGB(0xC7CCD1),
            "woodcutting", Color.fromRGB(0x5FBF52),
            "fishing", Color.fromRGB(0x3FC1C9));

    private static final Color UNCHARTED = Color.fromRGB(0xFFD98A);

    private SightingPalette() {
    }

    public static @NotNull Color of(@NotNull String templateKey) {
        return BY_TEMPLATE.getOrDefault(templateKey, UNCHARTED);
    }
}
