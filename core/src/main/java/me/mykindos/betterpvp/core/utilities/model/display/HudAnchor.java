package me.mykindos.betterpvp.core.utilities.model.display;

import net.kyori.adventure.text.format.TextColor;

/**
 * Which screen edge a HUD readout is pinned to by {@code rendertype_text.vsh}, and the near-white
 * sentinel colour that carries that instruction.
 *
 * <p>The shader cannot read an alpha flag (Adventure strips alpha to opaque), so the anchor flag is
 * hidden in the <em>low 3 bits</em> of each colour channel — a shift of at most 7/255, invisible to
 * the eye, that rides along with any colour (including a player head's skin pixels). Every glyph that
 * should be re-anchored must be drawn in this enum's {@link #color()} (or have its rgb run through
 * {@link #mark(int)} when it keeps its own colour, e.g. skin pixels).
 *
 * <p><strong>Must stay in lockstep with {@code bpvp_isMarkedLeft}/{@code bpvp_isMarkedRight} in
 * {@code rendertype_text.vsh}.</strong> The two signatures differ only in blue's low bit so both
 * read as the same near-white on screen:
 * <ul>
 *   <li>{@link #LEFT} — {@code (5,2,5)} → {@code 0xFDFAFD} — the clan/info HUD, left edge.</li>
 *   <li>{@link #RIGHT} — {@code (5,2,6)} → {@code 0xFDFAFE} — the world-event HUD, right edge.</li>
 * </ul>
 */
public enum HudAnchor {

    LEFT(5, 2, 5),
    RIGHT(5, 2, 6);

    private final int r;
    private final int g;
    private final int b;
    private final TextColor color;

    HudAnchor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.color = TextColor.color(mark(0xFFFFFF));
    }

    /** The near-white sentinel colour for this edge; draw re-anchored glyphs/text in it. */
    public TextColor color() {
        return color;
    }

    /**
     * Stamp this edge's signature into an arbitrary rgb, preserving the visible colour (only the low
     * 3 bits of each channel change). Used for glyphs that keep their own colour, e.g. skin pixels.
     */
    public int mark(int rgb) {
        final int red = (((rgb >> 16) & 0xFF) & ~7) | r;
        final int green = (((rgb >> 8) & 0xFF) & ~7) | g;
        final int blue = ((rgb & 0xFF) & ~7) | b;
        return (red << 16) | (green << 8) | blue;
    }
}
