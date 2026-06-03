package me.mykindos.betterpvp.core.world.zone.discovery;

import lombok.Builder;
import lombok.Value;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.world.zone.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

/**
 * Per-zone discovery notification config, attached to a {@link Zone} via its builder. Its mere presence marks a zone as
 * <i>discoverable</i> ({@link Zone#isDiscoverable()}) — the first time a player enters such a zone they get a title, a
 * sound and a chat line, and the moment is recorded in the database.
 * <p>
 * Sensible defaults mean {@code ZoneDiscovery.builder().build()} is enough for most zones: the title defaults to
 * {@link #DEFAULT_TITLE}, the sound to {@link #DEFAULT_SOUND}, and — because they depend on the zone — the subtitle
 * defaults to the zone's display name (white, undecorated) and the message to an "Unlocked area" line, both resolved at
 * notification time. Set any field to override its default.
 */
@Value
@Builder
public class ZoneDiscovery {

    /** Default title line shown on discovery. */
    public static final Component DEFAULT_TITLE = Component.text("Area Discovered", NamedTextColor.GOLD, TextDecoration.BOLD);
    /** Default sound played on discovery. */
    public static final SoundEffect DEFAULT_SOUND = new SoundEffect("minecraft", "ui.toast.challenge_complete", 1.1f);

    /** Title line. */
    @Builder.Default
    @Nullable Component title = DEFAULT_TITLE;
    /** Subtitle line; when {@code null} the notifier falls back to the zone's display name in white, undecorated. */
    @Nullable Component subtitle;
    /** Chat line; when {@code null} the notifier falls back to a default "Unlocked area" message. */
    @Nullable Component message;

    /** Sound played on discovery. */
    @Builder.Default
    SoundEffect sound = DEFAULT_SOUND;

    /** Title fade-in time in seconds. */
    @Builder.Default
    double fadeIn = 0.3;
    /** Title stay (visible) time in seconds. */
    @Builder.Default
    double stay = 5.0;
    /** Title fade-out time in seconds. */
    @Builder.Default
    double fadeOut = 0.5;
}
