package me.mykindos.betterpvp.core.cutscene.skip;

import me.mykindos.betterpvp.core.cutscene.CutsceneSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Whether this player may skip, and how much of the cutscene they may skip.
 * <p>
 * A policy is set on the cutscene and overridable on any single beat, which is what lets one moment inside an otherwise
 * skippable cutscene insist on being watched. Policies compose rather than enumerate, so a rule nobody has thought of
 * yet is a new combinator and not a new field on every cutscene.
 */
@FunctionalInterface
public interface SkipPolicy {

    Component BEAT_HINT = Component.text("Press SNEAK to skip ahead", NamedTextColor.GRAY);
    Component CUTSCENE_HINT = Component.text("Hold SNEAK to skip", NamedTextColor.GRAY);

    @NotNull SkipVerdict evaluate(@NotNull CutsceneSession session, @NotNull SkipScope scope);

    /** Nothing may be skipped. */
    static @NotNull SkipPolicy none() {
        return (session, scope) -> SkipVerdict.deny();
    }

    /** Individual beats may be skipped; the cutscene as a whole may not. */
    static @NotNull SkipPolicy beats() {
        return (session, scope) -> scope == SkipScope.BEAT ? SkipVerdict.allow(BEAT_HINT) : SkipVerdict.deny();
    }

    /** The cutscene may be skipped outright, but not stepped through beat by beat. */
    static @NotNull SkipPolicy whole() {
        return (session, scope) -> scope == SkipScope.CUTSCENE ? SkipVerdict.allow(CUTSCENE_HINT) : SkipVerdict.deny();
    }

    /** Both scopes are permitted. */
    static @NotNull SkipPolicy all() {
        return (session, scope) -> SkipVerdict.allow(scope == SkipScope.BEAT ? BEAT_HINT : CUTSCENE_HINT);
    }

    /** Defers to {@code inner}, but only for a player who satisfies {@code gate}. */
    static @NotNull SkipPolicy when(@NotNull Predicate<Player> gate, @NotNull SkipPolicy inner) {
        return (session, scope) -> {
            final Player player = session.getPlayer();
            return player != null && gate.test(player) ? inner.evaluate(session, scope) : SkipVerdict.deny();
        };
    }

    /**
     * Defers to {@code inner} only on a repeat viewing, so the first run through is always watched in full. The
     * session resolves "seen before" once when it starts, so this costs nothing per tick.
     */
    static @NotNull SkipPolicy seenBefore(@NotNull SkipPolicy inner) {
        return (session, scope) -> session.isSeenBefore() ? inner.evaluate(session, scope) : SkipVerdict.deny();
    }

    /** Allows a scope either policy allows, preferring {@code first}'s hint. */
    static @NotNull SkipPolicy either(@NotNull SkipPolicy first, @NotNull SkipPolicy second) {
        return (session, scope) -> {
            final SkipVerdict verdict = first.evaluate(session, scope);
            return verdict.isAllowed() ? verdict : second.evaluate(session, scope);
        };
    }
}
