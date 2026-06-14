package me.mykindos.betterpvp.core.framework.statusbar;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import me.mykindos.betterpvp.core.utilities.model.display.FontCanvas;
import me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Draws the in-combat HUD: a health bar, a mirrored mana bar, and the numeric readouts that float
 * above them, all painted onto the {@code status_bar.png} backdrop. Whatever the action bar had
 * queued (cooldown readouts, etc.) is appended to the right of the frame.
 * <p>
 * The frame is a centered action bar, so the client positions it by its total advance. Every glyph
 * and cursor shift here is paid back via {@link FontCanvas#space(int)} so the frame's net advance
 * stays fixed and it never drifts as health/energy change.
 */
public class StatusBar extends ActionBar {

    // Backdrop geometry (status_bar.png, advance 214px). The health fill sits on the left region,
    // which starts HEALTH_BAR_START in; the mana fill mirrors it on the right.
    private static final int BACKDROP_WIDTH = 214;
    private static final int HEALTH_BAR_START = 21;
    private static final int FILL_WIDTH = 79;
    // A 10-box bar is 39 glyph pieces of 2px each (the end cap adds 1px). The fill lights only the
    // leading (health) / trailing (mana) pieces the player's values warrant.
    private static final int BAR_PIECES = 39;

    private final EntityHealthService healthService;
    private final EnergyService energyService;
    private final CombatFeaturesService combatFeaturesService;
    private final EffectManager effectManager;

    public StatusBar(EntityHealthService healthService, EnergyService energyService, CombatFeaturesService combatFeaturesService, EffectManager effectManager) {
        this.healthService = healthService;
        this.energyService = energyService;
        this.combatFeaturesService = combatFeaturesService;
        this.effectManager = effectManager;
    }

    @Override
    public void show(Gamer gamer) {
        cleanUp();

        final Component queued = pollQueued(gamer);
        final Player viewer = gamer.getPlayer();
        final Component component = shouldDrawHud(viewer) ? renderHud(viewer, queued) : queued;

        sendActionBar(gamer, component);
    }

    /** The next queued action-bar message, or {@link #EMPTY} if nothing is waiting. */
    private Component pollQueued(Gamer gamer) {
        final Component queued;
        synchronized (lock) {
            queued = hasElementsQueued() ? nextComponent(gamer) : EMPTY;
        }
        return queued == null ? EMPTY : queued;
    }

    /** The combat HUD shows only for a present, vulnerable player with combat features active. */
    private boolean shouldDrawHud(Player viewer) {
        return viewer != null && !viewer.getGameMode().isInvulnerable() && combatFeaturesService.isActive(viewer);
    }

    /** Paint the full HUD frame and append the queued message to its right. */
    private Component renderHud(Player viewer, Component queued) {
        final FontCanvas canvas = new FontCanvas();
        final HealthBarPalette palette = resolveHealthPalette(viewer);
        drawBackdrop(canvas);
        drawHealthFill(canvas, viewer, palette);
        drawManaFill(canvas, viewer);
        drawReadouts(canvas, viewer, palette);
        appendQueued(canvas, viewer, queued);
        return canvas.build().shadowColor(ShadowColor.none());
    }

    // The default red palette, reused every tick a player has no tinting effect (the common case).
    private static final HealthBarPalette DEFAULT_PALETTE = new HealthBarPalette(
            TextColor.color(255, 76, 64),  // red base
            TextColor.color(251, 255, 0),  // yellow bonus
            TextColor.color(74, 29, 26));  // dark empty

    // The active health palette: the highest-priority tinting effect the viewer has (Poison → green,
    // Vulnerability → purple-grey, ...), or the default red palette when none is active. We walk the
    // pre-sorted tinting types and return the first one the viewer actually has, so the hot path is a
    // tiny short-circuiting loop rather than a full scan of every registered effect.
    private HealthBarPalette resolveHealthPalette(Player viewer) {
        for (EffectType type : tintingTypes()) {
            if (effectManager.hasEffect(viewer, type)) {
                return ((HealthBarTint) type).healthBarTint();
            }
        }
        return DEFAULT_PALETTE;
    }

    // The tinting effect types, highest priority first. Resolved once from the static EffectTypes
    // registry (fully populated long before the HUD renders) and cached — the set never changes at
    // runtime. The race on first render is benign: the computation is idempotent.
    private static volatile List<EffectType> tintingTypes;

    private static List<EffectType> tintingTypes() {
        List<EffectType> cached = tintingTypes;
        if (cached == null) {
            cached = EffectTypes.getEffectTypes().stream()
                    .filter(type -> type instanceof HealthBarTint)
                    .sorted(Comparator.comparingInt(type -> -((HealthBarTint) type).tintPriority()))
                    .toList();
            tintingTypes = cached;
        }
        return cached;
    }

    private void drawBackdrop(FontCanvas canvas) {
        canvas.space(-10);
        canvas.glyph('\uE012', NamedTextColor.WHITE, "hud/down_40").space(-1); // cancel the glyph's +1 advance
    }

    // Health fill — drawn left-to-right over the frame's health region. getHealth() already counts the
    // bonus, so capacity is maxHealth + bonus: the base portion lights red up to maxHealth and the
    // overflow lights the tail pieces yellow (the bonus pool drains first as the player takes damage).
    private void drawHealthFill(FontCanvas canvas, Player viewer, HealthBarPalette palette) {
        final double maxHealth = healthService.getMaxHealth(viewer);
        final double health = healthService.getHealth(viewer);
        final char[] pieces = healthPieces();
        final int filled = litPieces(Math.min(health, maxHealth), maxHealth);
        final int bonus = litPieces(Math.max(0, health - maxHealth), maxHealth);
        final int bonusStart = pieces.length - bonus;

        // Rewind onto the health region, light the pieces, then settle back to the frame's right edge.
        canvas.space(-(BACKDROP_WIDTH - HEALTH_BAR_START));
        for (int i = 0; i < pieces.length; i++) {
            final TextColor color = bonus > 0 && i >= bonusStart ? palette.getBonus()
                    : i < filled ? palette.getBase()
                    : palette.getEmpty();
            tile(canvas, pieces[i], color);
        }
        canvas.space(BACKDROP_WIDTH - HEALTH_BAR_START - FILL_WIDTH);
    }

    // Mana fill — mirror of the health bar on the backdrop's right region. getMaxEnergy() is the config
    // base cap (the bar spans this); getMax(id) can raise it per-player, and that boost pool drains
    // first and is drawn in a lighter blue over the centre (full) end of the bar.
    private void drawManaFill(FontCanvas canvas, Player viewer) {
        final double baseMaxEnergy = energyService.getMaxEnergy();
        final double energy = energyService.getEnergy(viewer.getUniqueId());
        final char[] pieces = manaPieces();
        final int filled = litPieces(Math.min(energy, baseMaxEnergy), baseMaxEnergy);
        final int boost = litPieces(Math.max(0, energy - baseMaxEnergy), baseMaxEnergy);
        // The mana region mirrors the health region across the backdrop.
        final int manaBarStart = BACKDROP_WIDTH - HEALTH_BAR_START - FILL_WIDTH + 9;

        // Rewind onto it, light the trailing pieces (mana drains toward the centre), then settle back.
        canvas.space(-(BACKDROP_WIDTH - manaBarStart));
        for (int i = 0; i < pieces.length; i++) {
            final TextColor color = boost > 0 && i < boost ? TextColor.color(130, 190, 255)     // lighter blue boost
                    : i >= pieces.length - filled ? TextColor.color(48, 114, 255)               // blue base
                    : TextColor.color(20, 40, 90);                                              // empty
            tile(canvas, pieces[i], color);
        }
        canvas.space(BACKDROP_WIDTH - manaBarStart - FILL_WIDTH);
    }

    // Numeric readouts above the bar — health over the left region, energy over the right. The base/max
    // is in the bar colour; any bonus/boost suffix matches the overflow fill colour.
    private void drawReadouts(FontCanvas canvas, Player viewer, HealthBarPalette palette) {
        final Key font = FontCanvas.font("offset/down_22");

        final int health = ceil(healthService.getHealth(viewer));
        final int maxHealth = ceil(healthService.getMaxHealth(viewer));
        final int bonus = ceil(healthService.getBonusHealth(viewer));
        drawReadout(canvas, 50, font, // health fill spans x[11,90]
                health + "/" + maxHealth, palette.getBase(),
                bonus == 0 ? "" : "+" + bonus, palette.getBonus());

        final int energy = ceil(energyService.getEnergy(viewer.getUniqueId()));
        final int maxEnergy = ceil(energyService.getMaxEnergy());
        final int boost = ceil(energyService.getMax(viewer.getUniqueId()) - energyService.getMaxEnergy());
        drawReadout(canvas, 152, font, // mana fill spans x[113,192]
                energy + "/" + maxEnergy, TextColor.color(48, 114, 255),
                boost <= 0 ? "" : "+" + boost, TextColor.color(130, 190, 255));
    }

    // Draw a "base+suffix" readout centred over a region, then settle back so the frame's advance (and
    // centering) is untouched. The two segments are adjacent components — each glyph advances a fixed
    // 6px regardless of colour, so the split leaves the readout's total advance unchanged.
    private void drawReadout(FontCanvas canvas, int center, Key font,
                             String base, TextColor baseColor, String suffix, TextColor suffixColor) {
        final int rightEdge = BACKDROP_WIDTH - 10; // cursor rest position after the frame
        final int advance = (base.length() + suffix.length()) * 6;
        final int left = center - (advance - 1) / 2; // the last glyph drops its trailing 1px gap
        canvas.space(left - rightEdge);
        canvas.append(shadowed(base, baseColor, font));
        if (!suffix.isEmpty()) {
            canvas.append(shadowed(suffix, suffixColor, font));
        }
        canvas.space(rightEdge - left - advance);
    }

    // Centre the queued message (cooldown readouts, etc.) over the frame. The cursor rests at the
    // frame's right edge, which equals the frame's total advance; we step left to a centred start,
    // draw, then settle back to the right edge. The two width-dependent shifts cancel, so the net
    // advance stays exactly the frame's width REGARDLESS of the measured width — the frame never
    // shifts even when the estimate is slightly off (bold text, block glyphs only affect centring).
    // The queued text floats above the bars: the HUD uses down-shifted fonts, so default-baseline
    // text sits higher and never overlaps it.
    private void appendQueued(FontCanvas canvas, Player viewer, Component queued) {
        final int width = UtilFont.componentWidth(Translations.render(queued, viewer.locale()));
        if (width <= 0) return;
        final int rightEdge = BACKDROP_WIDTH - 10;
        final int left = (rightEdge - width) / 2; // centre the message within [0, rightEdge]
        canvas.space(left - rightEdge);
        canvas.append(queued);
        canvas.space(rightEdge - left - width);
    }

    private void sendActionBar(Gamer gamer, Component component) {
        // Resolve any translatable nodes (e.g. cooldown ability names) into the recipient's locale
        // server-side so they never display untranslated/as raw keys.
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player != null) {
            player.sendActionBar(Translations.render(component, player.locale()));
        }
    }

    // Draw a single bar piece followed by space.-1, which cancels the 1px advance Minecraft inserts
    // after every glyph so consecutive pieces tile without a seam.
    private void tile(FontCanvas canvas, char glyph, TextColor color) {
        canvas.glyph(glyph, color, "hud/down_36").space(-1);
    }

    private static Component shadowed(String text, TextColor color, Key font) {
        return Component.text(text, color).font(font).shadowColor(ShadowColor.shadowColor(0xFF000000));
    }

    /** Number of lit pieces for {@code value} out of {@code max}, on the bar's 39-piece scale. */
    private static int litPieces(double value, double max) {
        if (max <= 0) return 0;
        return (int) Math.round(Math.clamp(value / max, 0.0, 1.0) * BAR_PIECES);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }

    // Left bar layout: box 1 opens with the start cap, boxes 2-9 with a divider, box 10 closes with
    // the end cap. Each box is start/divider + mid + mid (+ end cap for box 10).
    private static char[] healthPieces() {
        final char[] pieces = new char[BAR_PIECES];
        int p = 0;
        for (int box = 1; box <= 10; box++) {
            pieces[p++] = box == 1 ? '\uE00C' : '\uE00A';
            pieces[p++] = '\uE00A';
            pieces[p++] = '\uE00A';
            if (box >= 2) {
                pieces[p++] = box == 10 ? '\uE008' : '\uE00A';
            }
        }
        return pieces;
    }

    // Right bar layout: the bar_right_* glyphs are horizontal mirrors of the left ones, so the piece
    // order is reversed — the (mirrored) end cap sits on the left and the start cap on the right.
    private static char[] manaPieces() {
        final char[] pieces = new char[BAR_PIECES];
        int p = 0;
        for (int box = 1; box <= 10; box++) {
            pieces[p++] = box == 1 ? '\uE011' : '\uE00F';
            pieces[p++] = '\uE00F';
            pieces[p++] = '\uE00F';
            if (box >= 2) {
                pieces[p++] = box == 10 ? '\uE00D' : '\uE00F';
            }
        }
        return pieces;
    }
}
