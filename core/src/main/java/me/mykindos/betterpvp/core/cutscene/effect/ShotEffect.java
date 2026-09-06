package me.mykindos.betterpvp.core.cutscene.effect;

import me.mykindos.betterpvp.core.cutscene.CutsceneSession;
import org.jetbrains.annotations.NotNull;

/**
 * Something that is true for exactly as long as one beat is on screen: an arrow hanging over an NPC, a subtitle, a
 * glow, a sound.
 * <p>
 * {@link #exit} is guaranteed to run for any effect whose {@link #enter} ran - on a normal beat change, on a skip, and
 * on an abort - so an effect may hold whatever state it needs without also having to guess how its beat ended. Effects
 * are per-viewer by construction, because a session belongs to one player.
 */
public interface ShotEffect {

    void enter(@NotNull CutsceneSession session);

    default void exit(@NotNull CutsceneSession session) {
    }
}
