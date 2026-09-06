package me.mykindos.betterpvp.core.cutscene.signal;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * A named moment one track of a cutscene announces and another can wait on.
 * <p>
 * This is the whole of the bridge between the camera and the dialogue: neither track knows the other exists, they only
 * agree on names. Every element announces itself without being asked - a beat emits {@link #beat} on arrival and
 * {@link #beatEnd} on departure, a node emits {@link #node} and {@link #nodeEnd}, a response emits {@link #response} -
 * so the common cases are addressable without anybody writing a signal name at all. {@link #of} remains for a
 * rendezvous the derived names do not cover.
 */
@Value
public class Signal {

    @NotNull String key;

    /** A raw signal, for a rendezvous the derived names do not express. */
    public static @NotNull Signal of(@NotNull String key) {
        return new Signal(key.toLowerCase(Locale.ROOT));
    }

    /** The camera has arrived at {@code beatId} and the beat has begun. */
    public static @NotNull Signal beat(@NotNull String beatId) {
        return of("beat:" + beatId);
    }

    /**
     * The raw key for {@link #beat}, for handing to a dialogue node's {@code await}.
     * <p>
     * A node holds on a plain string because conversations must not import anything from this package - the dependency
     * runs one way. This is the one place the two vocabularies meet, so it is worth spelling out rather than leaving
     * authors to concatenate the prefix themselves.
     */
    public static @NotNull String beatKey(@NotNull String beatId) {
        return beat(beatId).getKey();
    }

    /** The beat {@code beatId} has finished. */
    public static @NotNull Signal beatEnd(@NotNull String beatId) {
        return of("beat:" + beatId + ":end");
    }

    /** The conversation has arrived at {@code nodeId} and its line has begun. */
    public static @NotNull Signal node(@NotNull String nodeId) {
        return of("node:" + nodeId);
    }

    /** The conversation has left {@code nodeId}, which happens when one of its responses is confirmed. */
    public static @NotNull Signal nodeEnd(@NotNull String nodeId) {
        return of("node:" + nodeId + ":end");
    }

    /** A specific response was chosen. */
    public static @NotNull Signal response(@NotNull String nodeId, @NotNull String responseId) {
        return of("response:" + nodeId + ":" + responseId);
    }

    /** The player pressed the cutscene's confirm input. */
    public static @NotNull Signal input() {
        return of("input");
    }

    /**
     * The dialogue track finished. Worth waiting on as a fallback: a beat expecting a node the conversation branched
     * away from would otherwise wait for something that is never coming.
     */
    public static @NotNull Signal conversationEnd() {
        return of("conversation:end");
    }

    @Override
    public String toString() {
        return key;
    }
}
