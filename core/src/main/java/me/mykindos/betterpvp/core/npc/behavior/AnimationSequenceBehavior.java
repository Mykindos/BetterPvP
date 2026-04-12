package me.mykindos.betterpvp.core.npc.behavior;

import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;

import java.util.List;

/**
 * Plays a sequence of ModelEngine animations on a {@link ModeledNPC} at a fixed interval.
 * <p>
 * Each tick the behaviour checks whether enough time has elapsed since the last animation
 * was played. If so, it advances to the next animation in the sequence (wrapping back to
 * the first after the last) and plays it on all {@link ActiveModel}s attached to the entity.
 *
 * <pre>{@code
 * npc.addBehavior(new AnimationSequenceBehavior(npc,
 *     List.of("idle_a", "idle_b", "idle_c"),
 *     3000L  // play a new animation every 3 seconds
 * ));
 * }</pre>
 */
public class AnimationSequenceBehavior implements NPCBehavior {

    private final ModeledNPC npc;
    private final List<String> sequence;
    private final long intervalMs;

    private int index = 0;
    private long lastPlayed = 0;

    /**
     * @param npc        The NPC whose models will be animated.
     * @param sequence   Ordered list of animation IDs to cycle through.
     * @param intervalMs Milliseconds between each animation step.
     */
    public AnimationSequenceBehavior(ModeledNPC npc, List<String> sequence, long intervalMs) {
        if (sequence.isEmpty()) throw new IllegalArgumentException("Animation sequence must not be empty");
        this.npc = npc;
        this.sequence = List.copyOf(sequence);
        this.intervalMs = intervalMs;
    }

    @Override
    public void tick() {
        if (System.currentTimeMillis() - lastPlayed < intervalMs) return;
        lastPlayed = System.currentTimeMillis();

        final String animationId = sequence.get(index);
        for (ActiveModel model : npc.getModeledEntity().getModels().values()) {
            ModelEngineHelper.playAnimation(model, animationId);
        }
        index = (index + 1) % sequence.size();
    }

}
