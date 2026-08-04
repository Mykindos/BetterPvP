package me.mykindos.betterpvp.clans.world.discovery.wind;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * How the wind reads to the crew: a factor turned into something worth saying.
 * <p>
 * Each band has several phrasings, so a long expedition through changeable weather does not report every shift in the
 * same sentence. The keys are numbered from one per band, which is what {@link #messageKey(int)} builds.
 */
@Getter
public enum WindBand {

    BECALMED(0.65),
    SLACK(0.90),
    FAIR(1.10),
    STRONG(1.35),
    GALE(Double.MAX_VALUE);

    /** How many phrasings this band has in the bundles. */
    private static final int VARIANTS = 3;

    /** Exclusive upper bound on the wind factor. The last band takes everything the others left. */
    private final double upperBound;

    WindBand(double upperBound) {
        this.upperBound = upperBound;
    }

    public static @NotNull WindBand of(double factor) {
        for (WindBand band : values()) {
            if (factor < band.upperBound) {
                return band;
            }
        }
        return GALE;
    }

    public int variants() {
        return VARIANTS;
    }

    /** One phrasing's translation key. Any {@code variant} is folded into range, so a caller may draw freely. */
    public @NotNull String messageKey(int variant) {
        return "clans.discovery.wind." + name().toLowerCase(Locale.ROOT) + "." + (Math.floorMod(variant, VARIANTS) + 1);
    }
}
