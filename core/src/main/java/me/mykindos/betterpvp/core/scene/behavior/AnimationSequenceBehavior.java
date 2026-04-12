package me.mykindos.betterpvp.core.scene.behavior;

import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.scene.HasModeledEntity;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;

import java.util.List;

/**
 * Plays a sequence of ModelEngine animations on a modeled scene entity at a fixed interval.
 * <p>
 * Each tick the behaviour checks whether enough time has elapsed since the last animation
 * was played. If so, it advances to the next animation in the sequence (wrapping back to
 * the first after the last) and plays it on all {@link ActiveModel}s attached to the entity.
 * <p>
 * Works on any entity implementing {@link HasModeledEntity}
 * ({@link me.mykindos.betterpvp.core.scene.npc.ModeledNPC} or
 * {@link me.mykindos.betterpvp.core.scene.prop.ModeledProp}).
 *
 * <pre>{@code
 * npc.addBehavior(new AnimationSequenceBehavior(npc,
 *     List.of("idle_a", "idle_b", "idle_c"),
 *     3000L  // play a new animation every 3 seconds
 * ));
 * }</pre>
 */
public class AnimationSequenceBehavior implements SceneBehavior {

    private final HasModeledEntity owner;
    private final List<String> sequence;
    private final long intervalMs;

    private int index = 0;
    private long lastPlayed = 0;

    /**
     * @param owner      The modeled entity whose models will be animated.
     * @param sequence   Ordered list of animation IDs to cycle through.
     * @param intervalMs Milliseconds between each animation step.
     */
    public AnimationSequenceBehavior(HasModeledEntity owner, List<String> sequence, long intervalMs) {
        if (sequence.isEmpty()) throw new IllegalArgumentException("Animation sequence must not be empty");
        this.owner = owner;
        this.sequence = List.copyOf(sequence);
        this.intervalMs = intervalMs;
    }

    @Override
    public void tick() {
        if (System.currentTimeMillis() - lastPlayed < intervalMs) return;
        lastPlayed = System.currentTimeMillis();

        final var modeledEntity = owner.getModeledEntity();
        if (modeledEntity == null) return;

        final String animationId = sequence.get(index);
        for (ActiveModel model : modeledEntity.getModels().values()) {
            ModelEngineHelper.playAnimation(model, animationId);
        }
        index = (index + 1) % sequence.size();
    }

}
