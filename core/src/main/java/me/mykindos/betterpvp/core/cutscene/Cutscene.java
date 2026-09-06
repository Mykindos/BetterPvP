package me.mykindos.betterpvp.core.cutscene;

import lombok.Getter;
import me.mykindos.betterpvp.core.cutscene.camera.Cinematic;
import me.mykindos.betterpvp.core.cutscene.skip.SkipPolicy;
import me.mykindos.betterpvp.core.quest.conversation.ConversationDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * One presentation: a camera track, a dialogue track, or both, played to one player.
 * <p>
 * Neither track owns the other. The camera does not drive the conversation and the conversation does not drive the
 * camera - they are peers held by one {@link CutsceneSession}, and they coordinate by announcing and waiting on
 * {@link me.mykindos.betterpvp.core.cutscene.signal.Signal}s. That is what makes both directions expressible: a beat
 * can wait for a dialogue branch, and a dialogue node can wait for the camera to arrive, in the same cutscene.
 * <p>
 * The session exists rather than the two tracks simply listening to each other because <em>skipping</em> needs a
 * single owner. Two independent lifecycles would let a skipped camera track strand the dialogue waiting on a signal
 * that is never coming; one session fast-forwards both together, tears down once, and restores the player once.
 *
 * <pre>{@code
 * Cutscene.builder("whats_new")
 *         .skip(SkipPolicy.seenBefore(SkipPolicy.all()))
 *         .dialogue(Conversations.builder("whats_new")....build())
 *         .camera(camera -> camera
 *                 .shot("whats_new/gate", s -> s.cut().until(Signal.response("greet", "show_me")))
 *                 .shot("whats_new/mine", s -> s.travel(2.5, Easing.EASE_IN_OUT)
 *                         .until(Signal.nodeEnd("mine_intro"))))
 *         .build();
 * }</pre>
 *
 * @see CutsceneManager#start(org.bukkit.entity.Player, Cutscene)
 */
@Getter
public final class Cutscene {

    private final String id;
    private final @Nullable Cinematic camera;
    private final @Nullable ConversationDefinition dialogue;
    private final SkipPolicy skip;

    /** Whether finishing this cutscene is remembered, which is what {@link SkipPolicy#seenBefore} reads. */
    private final boolean recordView;

    private Cutscene(@NotNull String id, @Nullable Cinematic camera, @Nullable ConversationDefinition dialogue,
                     @NotNull SkipPolicy skip, boolean recordView) {
        this.id = id;
        this.camera = camera;
        this.dialogue = dialogue;
        this.skip = skip;
        this.recordView = recordView;
    }

    public static @NotNull Builder builder(@NotNull String id) {
        return new Builder(id);
    }

    /**
     * A dialogue-only cutscene: no camera rig is spawned and the player is never put into spectator, so this behaves
     * exactly like starting the conversation directly - but it can still rendezvous, and a response inside it can
     * grow a camera track with {@code beginCinematic}.
     */
    public static @NotNull Cutscene of(@NotNull ConversationDefinition dialogue) {
        return builder(dialogue.getId()).dialogue(dialogue).build();
    }

    public boolean hasCamera() {
        return camera != null && !camera.isEmpty();
    }

    public boolean hasDialogue() {
        return dialogue != null;
    }

    public static final class Builder {

        private final String id;
        private @Nullable Cinematic camera;
        private @Nullable ConversationDefinition dialogue;
        private SkipPolicy skip = SkipPolicy.none();
        private boolean recordView = true;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public @NotNull Builder camera(@NotNull Cinematic cinematic) {
            this.camera = cinematic;
            return this;
        }

        /** Builds the camera track inline, for a cutscene whose shots are not shared with anything else. */
        public @NotNull Builder camera(@NotNull Consumer<Cinematic.Builder> cinematic) {
            final Cinematic.Builder builder = Cinematic.builder();
            cinematic.accept(builder);
            return camera(builder.build());
        }

        public @NotNull Builder dialogue(@NotNull ConversationDefinition definition) {
            this.dialogue = definition;
            return this;
        }

        /** The default policy for every beat that does not name its own. Unset means nothing may be skipped. */
        public @NotNull Builder skip(@NotNull SkipPolicy policy) {
            this.skip = policy;
            return this;
        }

        /** Stops this cutscene counting towards "seen before". For debug playbacks and anything replayed on demand. */
        public @NotNull Builder unrecorded() {
            this.recordView = false;
            return this;
        }

        public @NotNull Cutscene build() {
            return new Cutscene(id, camera, dialogue, skip, recordView);
        }
    }
}
