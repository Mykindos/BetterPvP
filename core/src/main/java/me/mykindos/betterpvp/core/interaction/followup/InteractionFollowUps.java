package me.mykindos.betterpvp.core.interaction.followup;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.Interaction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a set of follow-up interactions to execute after a parent interaction completes.
 * <p>
 * Follow-ups are organized by trigger condition:
 * <ul>
 *   <li>{@code onComplete} - executed when parent completes successfully</li>
 *   <li>{@code onFail} - executed when parent fails</li>
 *   <li>{@code then} - executed regardless of outcome (always)</li>
 * </ul>
 * <p>
 * Use {@link #builder()} to create instances with a fluent API.
 */
@Getter
public final class InteractionFollowUps {

    private final List<Interaction> onComplete;
    private final List<Interaction> onFail;
    private final List<Interaction> always;

    private InteractionFollowUps(List<Interaction> onComplete, List<Interaction> onFail, List<Interaction> always) {
        this.onComplete = Collections.unmodifiableList(onComplete);
        this.onFail = Collections.unmodifiableList(onFail);
        this.always = Collections.unmodifiableList(always);
    }

    /**
     * Check if this set has any follow-up interactions.
     *
     * @return true if any follow-ups are defined
     */
    public boolean hasFollowUps() {
        return !onComplete.isEmpty() || !onFail.isEmpty() || !always.isEmpty();
    }

    /**
     * Create a new builder for follow-up interactions.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for creating {@link InteractionFollowUps}.
     * <p>
     * The builder enforces type safety through its API:
     * <ul>
     *   <li>Multiple interactions can be added to each category</li>
     *   <li>Categories can be added in any order</li>
     *   <li>Call {@link #build()} to create the final immutable result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * InteractionFollowUps followUps = InteractionFollowUps.builder()
     *     .onComplete(healInteraction)
     *     .onComplete(playSuccessSound)
     *     .onFail(playFailSound)
     *     .then(logInteraction)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private final List<Interaction> onComplete = new ArrayList<>();
        private final List<Interaction> onFail = new ArrayList<>();
        private final List<Interaction> always = new ArrayList<>();

        private Builder() {
        }

        /**
         * Add an interaction to execute when the parent interaction completes successfully.
         *
         * @param interaction the interaction to execute on success
         * @return this builder
         */
        public Builder onComplete(@NotNull Interaction interaction) {
            Preconditions.checkNotNull(interaction, "Interaction cannot be null");
            onComplete.add(interaction);
            return this;
        }

        /**
         * Add multiple interactions to execute when the parent interaction completes successfully.
         *
         * @param interactions the interactions to execute on success
         * @return this builder
         */
        public Builder onComplete(@NotNull Interaction... interactions) {
            for (Interaction interaction : interactions) {
                onComplete(interaction);
            }
            return this;
        }

        /**
         * Add an interaction to execute when the parent interaction fails.
         *
         * @param interaction the interaction to execute on failure
         * @return this builder
         */
        public Builder onFail(@NotNull Interaction interaction) {
            Preconditions.checkNotNull(interaction, "Interaction cannot be null");
            onFail.add(interaction);
            return this;
        }

        /**
         * Add multiple interactions to execute when the parent interaction fails.
         *
         * @param interactions the interactions to execute on failure
         * @return this builder
         */
        public Builder onFail(@NotNull Interaction... interactions) {
            for (Interaction interaction : interactions) {
                onFail(interaction);
            }
            return this;
        }

        /**
         * Add an interaction to execute regardless of the parent interaction's outcome.
         * This is equivalent to the "then" callback - always executed after completion.
         *
         * @param interaction the interaction to always execute
         * @return this builder
         */
        public Builder then(@NotNull Interaction interaction) {
            Preconditions.checkNotNull(interaction, "Interaction cannot be null");
            always.add(interaction);
            return this;
        }

        /**
         * Add multiple interactions to execute regardless of the parent interaction's outcome.
         *
         * @param interactions the interactions to always execute
         * @return this builder
         */
        public Builder then(@NotNull Interaction... interactions) {
            for (Interaction interaction : interactions) {
                then(interaction);
            }
            return this;
        }

        /**
         * Build the immutable {@link InteractionFollowUps} instance.
         *
         * @return the built follow-ups
         * @throws IllegalStateException if no follow-ups were added
         */
        public InteractionFollowUps build() {
            Preconditions.checkState(!onComplete.isEmpty() || !onFail.isEmpty() || !always.isEmpty(),
                    "At least one follow-up interaction must be defined");
            return new InteractionFollowUps(
                    new ArrayList<>(onComplete),
                    new ArrayList<>(onFail),
                    new ArrayList<>(always)
            );
        }
    }
}
