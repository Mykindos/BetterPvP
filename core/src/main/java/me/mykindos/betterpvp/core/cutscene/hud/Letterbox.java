package me.mykindos.betterpvp.core.cutscene.hud;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.display.FontCanvas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * The black bars above and below a cutscene.
 * <p>
 * The two bars ride different HUD surfaces, and for one reason: each has to sit a fixed distance from <em>its own</em>
 * screen edge, and the distance between the edges is unknowable server-side - it depends on the client's resolution and
 * GUI scale. The boss-bar overlay is anchored a fixed number of GUI pixels below the top edge and the action bar a
 * fixed number above the bottom, so a bar hung off each is correct on every client without a shader. (The existing
 * {@code rendertype_text.vsh} anchor is horizontal only, and says as much: vertical placement never needed it.)
 * <p>
 * Thickness is not baked into the texture. The {@code betterpvp:cutscene} font carries the same two images at a ladder
 * of ascents, two pixels apart, so picking a bar height is picking a character - which is what makes the height a
 * config value that can be nudged and reloaded rather than a pack rebuild, and what makes the slide-in free: the
 * animation is just walking up the ladder.
 */
@Singleton
public class Letterbox {

    /** First character of the top bar's ascent ladder; the bottom bar's ladder starts at {@code BOTTOM_BASE}. */
    private static final char TOP_BASE = '\uE000';
    private static final char BOTTOM_BASE = '\uE040';

    /** Rungs on each ladder, and the pixels of bar thickness between consecutive rungs. */
    private static final int STEPS = 49;
    private static final int PIXELS_PER_STEP = 2;

    /** One glyph is 256px wide; twelve of them cover an ultrawide screen at the smallest GUI scale. */
    private static final int TILE_WIDTH = 256;
    private static final int TILES = 12;

    @Inject
    @Config(path = "cutscene.letterbox.topHeight", defaultValue = "65")
    private int topHeight;

    @Inject
    @Config(path = "cutscene.letterbox.bottomHeight", defaultValue = "56")
    private int bottomHeight;

    /** How long the bars take to slide in. Zero snaps them on instantly. */
    @Inject
    @Config(path = "cutscene.letterbox.slideTicks", defaultValue = "6")
    private int slideTicks;

    /**
     * The top bar as it looks {@code ticksElapsed} ticks into a cutscene.
     *
     * @param ticksElapsed ticks since the cutscene started, which drives the slide
     */
    public @NotNull Component top(int ticksElapsed) {
        return band(TOP_BASE, step(topHeight, ticksElapsed));
    }

    /** @see #top(int) */
    public @NotNull Component bottom(int ticksElapsed) {
        return band(BOTTOM_BASE, step(bottomHeight, ticksElapsed));
    }

    /** Whether a bar is drawn at all at this thickness - a height of zero means the cutscene wants no letterbox. */
    public boolean hasBars() {
        return topHeight > 0 || bottomHeight > 0;
    }

    /** How far up the ascent ladder this bar has climbed, given its target height and how long it has been sliding. */
    private int step(int height, int ticksElapsed) {
        final int target = Math.clamp(height / PIXELS_PER_STEP, 0, STEPS - 1);
        if (slideTicks <= 0 || ticksElapsed >= slideTicks) {
            return target;
        }
        return (int) Math.round(target * (double) ticksElapsed / slideTicks);
    }

    /**
     * Tiles one bar glyph across the screen as a net-zero block.
     * <p>
     * Each tile pays back the font's one pixel of glyph spacing so the tiles butt up seamlessly, the whole run is
     * displaced left by half its width so it centres on the action bar's own centring, and the run's total advance is
     * paid back at the end - otherwise the bar would push everything drawn beside it off-centre.
     */
    private @NotNull Component band(char base, int step) {
        if (step <= 0) {
            return Component.empty();
        }

        final char glyph = (char) (base + step);
        final TextComponent.Builder tiles = Component.text();
        for (int i = 0; i < TILES; i++) {
            tiles.append(Component.text(glyph, NamedTextColor.WHITE).font(FontCanvas.font("cutscene"))
                    .append(Component.translatable("space.-1").font(Resources.Font.SPACE)));
        }

        final int width = TILE_WIDTH * TILES;
        return Component.text()
                .append(Component.translatable("offset.-" + width / 2, tiles.build()).font(Resources.Font.SPACE))
                .append(Component.translatable("space." + -width).font(Resources.Font.SPACE))
                .build();
    }
}
